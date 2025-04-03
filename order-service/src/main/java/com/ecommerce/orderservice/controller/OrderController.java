package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.security.UserPrincipal;
import com.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OrderCreateRequest orderRequest) {

        Long userId = principal.getId();
        log.debug("Creating order for user ID: {}", userId);

        OrderDTO createdOrder = orderService.createOrder(userId, orderRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdOrder.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdOrder);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == @orderService.getOrderById(#id).userId")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        log.debug("Getting order by ID: {}", id);
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == @orderService.getOrderByOrderNumber(#orderNumber).userId")
    public ResponseEntity<OrderDTO> getOrderByNumber(@PathVariable String orderNumber) {
        log.debug("Getting order by number: {}", orderNumber);
        return ResponseEntity.ok(orderService.getOrderByOrderNumber(orderNumber));
    }

    @GetMapping
    public ResponseEntity<Page<OrderDTO>> getUserOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Long userId = principal.getId();
        log.debug("Getting orders for user ID: {}", userId);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(orderService.getUserOrders(userId, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<OrderDTO>> getUserOrdersByStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Long userId = principal.getId();
        log.debug("Getting orders for user ID: {} with status: {}", userId, status);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(orderService.getUserOrdersByStatus(userId, status, pageable));
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDTO>> getAllOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.debug("Getting all orders with status: {}", status);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(orderService.getAllOrdersByStatus(status, pageable));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest statusUpdateRequest) {

        log.debug("Updating status for order ID: {} to {}", id, statusUpdateRequest.getStatus());

        OrderDTO updatedOrder = orderService.updateOrderStatus(id, statusUpdateRequest);

        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("authentication.principal.id == @orderService.getOrderById(#id).userId")
    public ResponseEntity<OrderDTO> processPayment(
            @PathVariable Long id,
            @Valid @RequestBody OrderPaymentRequest paymentRequest) {

        log.debug("Processing payment for order ID: {}", id);

        OrderDTO updatedOrder = orderService.processPayment(id, paymentRequest);

        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == @orderService.getOrderById(#id).userId")
    public ResponseEntity<OrderDTO> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {

        log.debug("Cancelling order ID: {} with reason: {}", id, reason);

        OrderDTO cancelledOrder = orderService.cancelOrder(id, reason);

        return ResponseEntity.ok(cancelledOrder);
    }
}