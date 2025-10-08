package org.upnext.orderservice.Services;

import org.springframework.web.util.UriComponentsBuilder;
import org.upnext.orderservice.Models.Order;
import org.upnext.sharedlibrary.Dtos.*;
import org.upnext.sharedlibrary.Errors.Result;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    Optional<Order> getOrderObjectById(Long id);

    Result<OrderDto> getOrderById(UserDto userDto, Long id);

    Result<List<OrderDto>> getAllOrders();// for admins

    Result<List<OrderDto>> getUserOrders(Long userId);

    Result<OrderPaymentResponse> placeOrder(UserDto userDto, OrderPaymentRequest orderPaymentRequest, UriComponentsBuilder urb) throws Exception;

    Result<URI> updateOrderStatus(Long id, OrderStatusRequest orderStatusRequest, UriComponentsBuilder urb);

    Result<?> updateOrderStatus(Long id, OrderStatusRequest orderStatusRequest);

    Result<URI> cancelOrder(UserDto  userDto, Long id, UriComponentsBuilder urb);

}
