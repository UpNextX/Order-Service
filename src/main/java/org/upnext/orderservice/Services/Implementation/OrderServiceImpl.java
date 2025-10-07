package org.upnext.orderservice.Services.Implementation;


import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.upnext.orderservice.Clients.CartClient;
import org.upnext.orderservice.Clients.ProductClient;
import org.upnext.orderservice.Mappers.OrderMapper;
import org.upnext.orderservice.Models.Order;
import org.upnext.orderservice.Repositories.OrderRepository;
import org.upnext.orderservice.Services.OrderService;
import org.upnext.sharedlibrary.Dtos.*;
import org.upnext.sharedlibrary.Enums.OrderStatus;
import org.upnext.sharedlibrary.Enums.PaymentStatus;
import org.upnext.sharedlibrary.Errors.Error;
import org.upnext.sharedlibrary.Errors.Result;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.upnext.orderservice.Errors.OrderErrors.OrderNotFound;
import static org.upnext.orderservice.Errors.OrderErrors.ProductStockInSufficient;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartClient cartClient;
    private final ProductClient productClient;

    OrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper, CartClient cartClient, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.cartClient = cartClient;
        this.productClient = productClient;
    }

    Optional<Order> getOrderObjectById(long id) {
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
    public Result<OrderPaymentDto> placeOrder(@Valid UserDto userDto, UriComponentsBuilder urb) {
        CartDto cartDto = cartClient.getCart();
        if (!checkStock(cartDto)) {
            return Result.failure(ProductStockInSufficient);
        }
        Double totalCost = cartDto.getTotalCost();

        Order order = orderMapper.fromCartDto(cartDto);
        order = orderRepository.save(order);
        OrderPaymentDto orderPaymentDto = orderMapper.toOrderPaymentDto(order);
        updateStock(order, -1);
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

    private void updateStock(Order order, Integer factor) {
        order.getItems().forEach(item -> {
            productClient.updateStock(item.getProductId(), new StockUpdateRequest(factor * item.getQuantity()));
        });
    }

    @Transactional
    public Result<URI> updateOrder(@Valid UserDto userDto, @Valid OrderPaymentDto orderPaymentDto, UriComponentsBuilder urb) {
        Optional<Order> orderOpt = orderRepository.findById(orderPaymentDto.getOrderId());
        if (orderOpt.isEmpty() || !orderOpt.get().getId().equals(userDto.getId())) {
            return Result.failure(OrderNotFound);
        }
        URI uri;
        Order order = orderOpt.get();
        if (orderPaymentDto.getPaymentStatus() != PaymentStatus.CANCELED) {
            order.setPaymentStatus(orderPaymentDto.getPaymentStatus());
            order.setPaymentMethod(orderPaymentDto.getPaymentMethod());
            order.setPaymentTransactionId(orderPaymentDto.getPaymentTransactionId());
            orderRepository.save(order);
            uri = urb.path("/orders/{id}")
                    .buildAndExpand(order.getId())
                    .toUri();
            cartClient.clearCart();
        } else {
            order.setPaymentStatus(PaymentStatus.CANCELED);
            order.setOrderStatus(OrderStatus.CANCELED);
            orderRepository.save(order);
            uri = urb.path("/orders/{id}}")
                    .buildAndExpand(order.getId())
                    .toUri();
            updateStock(order, 1);
        }
        return Result.success(uri);
    }

    @Override
    @Transactional
    public Result<URI> cancelOrder(UserDto userDto, Long id, UriComponentsBuilder urb) {
        Optional<Order> orderOpt = orderRepository.findById(id);

        if (orderOpt.isEmpty() || !orderOpt.get().getUserId().equals(userDto.getId())) {

            return Result.failure(OrderNotFound);
        }
        if(orderOpt.get().getOrderStatus() == OrderStatus.DELIVERED){
            return Result.failure(new Error("Order.DELIVERED", "Can't cancel delivered order", 403));
        }
        URI uri;
        Order order = orderOpt.get();
        order.setOrderStatus(OrderStatus.CANCELED);
        order.setPaymentStatus(PaymentStatus.CANCELED);
        updateStock(order, 1);
        orderRepository.save(order);
         uri = urb.path("/orders/{id}")
                .buildAndExpand(order.getId())
                .toUri();
        return Result.success(uri);
    }


    private void fillProductDto(OrderDto orderDto) {
        orderDto.getOrderItems()
                .forEach(orderItemDto -> orderItemDto.setProduct(productClient.getProduct(orderItemDto.getId())));
    }

}
