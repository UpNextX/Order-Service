package org.upnext.orderservice.Services;

import org.springframework.web.util.UriComponentsBuilder;
import org.upnext.sharedlibrary.Dtos.OrderDto;
import org.upnext.sharedlibrary.Dtos.OrderPaymentDto;
import org.upnext.sharedlibrary.Dtos.OrderPaymentRequest;
import org.upnext.sharedlibrary.Dtos.OrderPaymentResponse;
import org.upnext.sharedlibrary.Dtos.UserDto;
import org.upnext.sharedlibrary.Errors.Result;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    Result<OrderDto> getOrderById(UserDto userDto, Long id);

    Result<List<OrderDto>> getAllOrders();// for admins

    Result<List<OrderDto>> getUserOrders(Long userId);

    Result<OrderPaymentResponse> placeOrder(UserDto userDto, UriComponentsBuilder urb);

    Result<URI> updateOrder(UserDto userDto, OrderPaymentRequest orderPaymentRequest, UriComponentsBuilder urb);

    Result<URI> cancelOrder(UserDto  userDto, Long id, UriComponentsBuilder urb);
}
