package org.upnext.orderservice.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
        name = "Order Management"
)
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @Operation(
            summary = "Get all orders (Admin only)",
            description = "Returns a list of all orders in the system. Accessible only by users with the ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved orders", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied"),
    })
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

    @Operation(
            summary = "Get order by ID",
            description = "Retrieve a specific order by its ID. Admins can access any order, while users can only access their own."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
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

    @Operation(
            summary = "Get current user's orders",
            description = "Retrieve all orders placed by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
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

    @Operation(
            summary = "Get all orders for a specific user (Admin only)",
            description = "Retrieve all orders made by a specific user, identified by userId. Accessible only by admins."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
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

    @Operation(
            summary = "Prepare a new order for payment",
            description = "Creates a new order based on the user's cart and generates a Stripe payment session URL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order prepared successfully", content = @Content(schema = @Schema(implementation = OrderPaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or empty cart"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/prepare")
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody OrderPaymentRequest orderPaymentRequest,
            @AuthenticationPrincipal UserDto user,
            UriComponentsBuilder urb) throws Exception {
        Result<OrderPaymentResponse> order = orderService.placeOrder(user, orderPaymentRequest, urb);
        if (order.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(order.getValue());
        }
        return ResponseEntity.status(order.getError().getStatusCode())
                .body(order.getError().getMessage());
    }

    @Operation(
            summary = "Update order status (Admin only)",
            description = "Allows an admin to update the order status or payment status of a specific order."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Order status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(
            @Parameter(description = "ID of the order to update", example = "3")

            @PathVariable Long id,
            @Schema(
                    description = "Request body containing updated order and payment statuses.",
                    implementation = OrderStatusRequest.class
            )
            @Valid @RequestBody OrderStatusRequest orderStatusRequest,
            @AuthenticationPrincipal UserDto user, UriComponentsBuilder urb) {

        Result<URI> result = orderService.updateOrderStatus(id, orderStatusRequest, urb);
        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(result.getValue());
        }
        return ResponseEntity.status(result.getError().getStatusCode())
                .body(result.getError().getMessage());
    }

    @Operation(
            summary = "Cancel an order",
            description = "Cancels an existing order if it hasn't been delivered yet. Only the user who placed the order can cancel it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized to cancel this order"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
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
