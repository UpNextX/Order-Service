package org.upnext.orderservice.Controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.upnext.orderservice.Annotations.RequiredRole;
import org.upnext.orderservice.Services.OrderService;
import org.upnext.sharedlibrary.Dtos.*;
import org.upnext.sharedlibrary.Enums.OrderStatus;
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
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }

    @RequiredRole("AUTHENTICATED")
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(HttpServletRequest request, @PathVariable Long id) {
        UserDto user = userExtractor(request);

        Result<OrderDto> result = orderService.getOrderById(user, id);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }

    @RequiredRole("AUTHENTICATED")
    @GetMapping("/me")
    public ResponseEntity<?> getUserOrders(HttpServletRequest request) {
        UserDto user = userExtractor(request);

        Result<List<OrderDto>> result = orderService.getUserOrders(user.getId());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }

        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }

    @RequiredRole("AUTHENTICATED")
    @PostMapping("/me/prepare")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderPaymentRequest orderPaymentRequest, HttpServletRequest request, UriComponentsBuilder urb) throws Exception {
        UserDto user = userExtractor(request);
        Result<OrderPaymentResponse> order = orderService.placeOrder(user, orderPaymentRequest, urb);
        if (order.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(order.getValue());
        }
        return ResponseEntity.status(order.getError().getStatusCode())
                .body(order.getError().getMessage());
    }

    @RequiredRole("ADMIN")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest orderStatusRequest, HttpServletRequest request, UriComponentsBuilder urb) {
        Result<URI> result = orderService.updateOrderStatus(id, orderStatusRequest, urb);
        if(result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }


    @RequiredRole("AUTHENTICATED")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, HttpServletRequest request, UriComponentsBuilder urb) {
        UserDto user = userExtractor(request);

        Result<URI> result = orderService.cancelOrder(user, id, urb);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }


}
