package org.upnext.orderservice.Services.Implementation;


import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.NullArgumentException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.upnext.orderservice.Clients.CartClient;
import org.upnext.orderservice.Clients.ProductClient;
import org.upnext.orderservice.Exceptions.EmptyCartException;
import org.upnext.orderservice.Exceptions.OrderNotFoundException;
import org.upnext.orderservice.Exceptions.OrderStatusException;
import org.upnext.orderservice.Exceptions.ProductStockException;
import org.upnext.orderservice.Mappers.OrderMapper;
import org.upnext.orderservice.Models.Order;
import org.upnext.orderservice.Repositories.OrderRepository;
import org.upnext.orderservice.Services.OrderService;
import org.upnext.sharedlibrary.Dtos.*;
import org.upnext.sharedlibrary.Enums.OrderStatus;
import org.upnext.sharedlibrary.Enums.PaymentMethod;
import org.upnext.sharedlibrary.Enums.PaymentStatus;
import org.upnext.sharedlibrary.Errors.Result;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.upnext.orderservice.Configurations.PaymentRabbitMqConfig.*;
import static org.upnext.orderservice.Errors.OrderErrors.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartClient cartClient;
    private final ProductClient productClient;

    @Lazy
    private final StripePaymentService stripePaymentService;

    @Override
    public Optional<Order> getOrderObjectById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public Result<OrderDto> getOrderById(UserDto userDto, Long id) {
        Optional<Order> orderOptional = getOrderObjectById(id);

        if (orderOptional.isEmpty() || (!userDto.getRole().equals("ADMIN") && !userDto.getId().equals(orderOptional.get().getUserId()))) {
            return Result.failure(OrderNotFound);
        }

        Order order = orderOptional.get();
        OrderDto orderDto = orderMapper.toOrderDto(order);
        fillProductDto(orderDto);

        return Result.success(orderDto);
    }

    @Override
    public Result<List<OrderDto>> getAllOrders() {
        List<OrderDto> orders = orderRepository.findAll()
                .stream()
                .map(orderMapper::toOrderDto)
                .toList();

        orders.forEach(this::fillProductDto);

        return Result.success(orders);
    }

    @Override
    public Result<List<OrderDto>> getUserOrders(Long userId) {
        List<OrderDto> orders = orderRepository.findAllByUserId(userId)
                .stream()
                .map(orderMapper::toOrderDto)
                .toList();

        orders.forEach(this::fillProductDto);

        return Result.success(orders);
    }

    @Override
    @Transactional
    public Result<OrderPaymentResponse> placeOrder(@Valid UserDto userDto, OrderPaymentRequest orderPaymentRequest, UriComponentsBuilder urb) throws StripeException {

        CartDto cartDto = cartClient.getCart();

        if (!checkStock(cartDto)) {
            throw new ProductStockException("InSufficient Stock");
        }

        Double totalCost = cartDto.getTotalCost();

        Order order = orderMapper.fromCartDto(cartDto);
        if(order.getItems().isEmpty()) {
            throw  new EmptyCartException("Empty Cart");
        }
        order = orderRepository.save(order);

        order.setPaymentMethod(PaymentMethod.CARD);
        Session session = stripePaymentService.createSession(order);
        order.setPaymentTransactionId(session.getId());

        order = orderRepository.save(order);
        OrderPaymentResponse orderPaymentDto = new OrderPaymentResponse();
        orderPaymentDto.setOrderId(order.getId());
        orderPaymentDto.setAmount(totalCost);
        orderPaymentDto.setUserId(userDto.getId());
        orderPaymentDto.setUrl(session.getUrl());

        decreaseStock(order);

        return Result.success(orderPaymentDto);

    }

    private boolean checkStock(CartDto cartDto) {
        for (CartItemDto item : cartDto.getItems()) {
            if (item.getQuantity() > item.getProduct().getStock()) {
                return false;
            }
        }
        return true;
    }

    private void increaseStock(Order order) {
        order.getItems().forEach(item -> {
            updateStock(item.getProductId(), item.getQuantity(), 1);
        });
    }

    private void decreaseStock(Order order) {
        order.getItems().forEach(item -> {
            updateStock(item.getProductId(), item.getQuantity(), -1);
        });    }

    private void updateStock(Long productId, Integer quantity, Integer factor) {
        productClient.updateStock(productId, new StockUpdateRequest(factor * quantity));
    }

    @Override
    @Transactional
    public Result<URI> updateOrderStatus(Long id, OrderStatusRequest orderStatusRequest, UriComponentsBuilder urb) {


        Result<?> result = updateOrderStatus(id, orderStatusRequest);
        if(result.getIsFailure()){
            return Result.failure(result.getError());
        }
        URI uri = urb.path("/orders/{id}").buildAndExpand(id).toUri();
        return Result.success(uri);
    }

    @Override
    @Transactional
    public Result<?> updateOrderStatus(Long id, OrderStatusRequest orderStatusRequest) {
        Order order = getOrderObjectById(id).orElse(null);
        if (order == null) {
            throw new OrderNotFoundException("Order not found");
        }
        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new OrderStatusException("Can't change order status");
        }


        order.setOrderStatus(orderStatusRequest.getOrderStatus());
        order.setPaymentStatus(orderStatusRequest.getPaymentStatus());
        orderRepository.save(order);
        if(orderStatusRequest.getPaymentStatus() == null || orderStatusRequest.getOrderStatus() == null ) {
            throw new NullArgumentException("Order status or order payment status is null");
        }

        return Result.success();
    }

    @RabbitListener(queues = SUCCESS_QUEUE)
    @Transactional
    public void orderPaymentSuccess(SuccessfulPaymentEvent successfulPaymentEvent) {
        Long orderId = successfulPaymentEvent.getOrderId();
        Order order = getOrderObjectById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        System.out.println("Order Payment Success"+ successfulPaymentEvent);

        OrderStatusRequest orderStatusRequest = OrderStatusRequest.builder()
                                                .orderId(orderId)
                                                .orderStatus(OrderStatus.CONFIRMED)
                                                .paymentStatus(PaymentStatus.PAID)
                                                        .build();

        updateOrderStatus(orderId, orderStatusRequest);
    }

    @RabbitListener(queues = FAILURE_QUEUE)
    @Transactional
    public void orderPaymentFailure(SuccessfulPaymentEvent successfulPaymentEvent) {
        Long orderId = successfulPaymentEvent.getOrderId();
        Order order = getOrderObjectById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        increaseStock(order);
        OrderStatusRequest orderStatusRequest = new OrderStatusRequest();
        OrderStatusRequest.builder()
                .orderId(orderId)
                .orderStatus(OrderStatus.CANCELED)
                .paymentStatus(PaymentStatus.FAILED)
                        .build();
        updateOrderStatus(orderId, orderStatusRequest);
    }



    @Override
    @Transactional
    public Result<URI> cancelOrder(UserDto userDto, Long id, UriComponentsBuilder urb) {
        Optional<Order> orderOpt = orderRepository.findById(id);

        if (orderOpt.isEmpty() || !orderOpt.get().getUserId().equals(userDto.getId())) {

            return Result.failure(OrderNotFound);
        }
        if (orderOpt.get().getOrderStatus() == OrderStatus.DELIVERED) {
            throw new OrderStatusException("Can't change order status");
        }
        if(orderOpt.get().getOrderStatus() == OrderStatus.CANCELED) {
            throw new OrderStatusException("Order Already Canceled");
        }
        URI uri;
        Order order = orderOpt.get();
        order.setOrderStatus(OrderStatus.CANCELED);
        order.setPaymentStatus(PaymentStatus.CANCELED);
        increaseStock(order);
        orderRepository.save(order);
        uri = urb.path("/orders/{id}")
                .buildAndExpand(order.getId())
                .toUri();
        return Result.success(uri);
    }


    private void fillProductDto(OrderDto orderDto) {
        orderDto.getOrderItems()
                .forEach(
                        orderItemDto -> orderItemDto.setProduct(productClient.getProduct(orderItemDto.getProduct().getId())));
    }

}
