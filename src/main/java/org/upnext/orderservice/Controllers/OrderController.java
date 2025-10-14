package org.upnext.orderservice.Controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.upnext.orderservice.Services.OrderService;
import org.upnext.sharedlibrary.Dtos.*;
import org.upnext.sharedlibrary.Errors.Result;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getOrders(HttpServletRequest request, @AuthenticationPrincipal UserDto user) {

        Result<List<OrderDto>> result = orderService.getAllOrders();
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@AuthenticationPrincipal UserDto user, @PathVariable Long id) {

        Result<OrderDto> result = orderService.getOrderById(user, id);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<?> getUserOrders(@AuthenticationPrincipal UserDto user) {

        Result<List<OrderDto>> result = orderService.getUserOrders(user.getId());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }

        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(@AuthenticationPrincipal UserDto user, @PathVariable Long userId) {
        Result<List<OrderDto>> result = orderService.getUserOrders(userId);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/prepare")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderPaymentRequest orderPaymentRequest, @AuthenticationPrincipal UserDto user, UriComponentsBuilder urb) throws Exception {
        Result<OrderPaymentResponse> order = orderService.placeOrder(user, orderPaymentRequest, urb);
        if (order.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(order.getValue());
        }
        return ResponseEntity.status(order.getError().getStatusCode())
                .body(order.getError().getMessage());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest orderStatusRequest, @AuthenticationPrincipal UserDto user, UriComponentsBuilder urb) {
        Result<URI> result = orderService.updateOrderStatus(id, orderStatusRequest, urb);
        if(result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }


    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, @AuthenticationPrincipal UserDto user, UriComponentsBuilder urb) {

        Result<URI> result = orderService.cancelOrder(user, id, urb);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }


}
