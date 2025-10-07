package org.upnext.orderservice.Controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.upnext.orderservice.Annotations.RequiredRole;
import org.upnext.orderservice.Services.OrderService;
import org.upnext.sharedlibrary.Dtos.OrderDto;
import org.upnext.sharedlibrary.Dtos.OrderPaymentDto;
import org.upnext.sharedlibrary.Dtos.UserDto;
import org.upnext.sharedlibrary.Errors.Result;

import java.net.URI;
import java.util.List;

import static org.upnext.orderservice.Utils.UserExtractor.userExtractor;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @RequiredRole("ADMIN")
    @GetMapping
    public ResponseEntity<?> getOrders(HttpServletRequest request) {
        UserDto user = userExtractor(request);

        Result<List<OrderDto>> result = orderService.getAllOrders();
        if(result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError());
    }

    @RequiredRole("AUTHENTICATED")
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(HttpServletRequest request, @PathVariable Long id) {
        UserDto user = userExtractor(request);

        Result<OrderDto> result = orderService.getOrderById(user, id);
        if(result.isSuccess()) {
            return  ResponseEntity.ok(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError());
    }

    @RequiredRole("AUTHENTICATED")
    @GetMapping("/me")
    public ResponseEntity<?> getUserOrders(HttpServletRequest request) {
        UserDto user = userExtractor(request);

        Result<List<OrderDto>> result = orderService.getUserOrders(user.getId());

        if(result.isSuccess()) {
            return  ResponseEntity.ok(result.getValue());
        }

        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError());
    }

    @RequiredRole("AUTHENTICATED")
    @PostMapping("/me/prepare")
    public ResponseEntity<?> createOrder(HttpServletRequest request, UriComponentsBuilder urb) {
        UserDto user = userExtractor(request);
        Result<OrderPaymentDto> order = orderService.placeOrder(user, urb);
        if(order.isSuccess()) {
            return ResponseEntity.ok(order.getValue());
        }
        return ResponseEntity.status(order.getError().getStatusCode())
                .body(order.getError());
    }

    @RequiredRole("AUTHENTICATED")
    @PutMapping("/id")
    public ResponseEntity<?> updateOrder(@Valid @RequestBody OrderPaymentDto orderPaymentDto, HttpServletRequest request, UriComponentsBuilder urb) {
        UserDto user = userExtractor(request);

        Result<URI> result = orderService.updateOrder(user, orderPaymentDto, urb);
        if(result.isSuccess()) {
            return  ResponseEntity.ok(result.getValue());
        }

        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError());
    }


    @RequiredRole("AUTHENTICATED")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, HttpServletRequest request, UriComponentsBuilder urb) {
        UserDto user = userExtractor(request);

        Result<URI> result = orderService.cancelOrder(user, id, urb);
        if(result.isSuccess()) {
            return  ResponseEntity.ok(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError());
    }


}
