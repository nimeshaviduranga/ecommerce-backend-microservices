package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.*;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.security.UserPrincipal;
import com.ecommerce.paymentservice.service.PaymentService;
import com.ecommerce.paymentservice.service.RefundService;
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

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PaymentRequestDTO paymentRequest) {

        log.debug("Creating payment for user: {}", principal.getId());

        PaymentResponseDTO payment = paymentService.createPayment(principal.getId(), paymentRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(payment.getPaymentId())
                .toUri();

        return ResponseEntity.created(location).body(payment);
    }

    @PostMapping("/{paymentId}/process")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @PathVariable String paymentId,
            @RequestParam String paymentMethodId) {

        log.debug("Processing payment: {} with method: {}", paymentId, paymentMethodId);

        PaymentResponseDTO payment = paymentService.processPayment(paymentId, paymentMethodId);

        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("@securityService.canAccessPayment(#paymentId)")
    public ResponseEntity<PaymentResponseDTO> getPayment(@PathVariable String paymentId) {
        log.debug("Getting payment: {}", paymentId);

        PaymentResponseDTO payment = paymentService.getPayment(paymentId);

        return ResponseEntity.ok(payment);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("@securityService.canAccessOrder(#orderId)")
    public ResponseEntity<PaymentResponseDTO> getPaymentByOrderId(@PathVariable String orderId) {
        log.debug("Getting payment for order: {}", orderId);

        PaymentResponseDTO payment = paymentService.getPaymentByOrderId(orderId);

        return ResponseEntity.ok(payment);
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponseDTO>> getUserPayments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.debug("Getting payments for user: {}", principal.getId());

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<PaymentResponseDTO> payments = paymentService.getUserPayments(principal.getId(), pageable);

        return ResponseEntity.ok(payments);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<PaymentResponseDTO>> getUserPaymentsByStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.debug("Getting payments for user: {} with status: {}", principal.getId(), status);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<PaymentResponseDTO> payments = paymentService.getUserPaymentsByStatus(principal.getId(), status, pageable);

        return ResponseEntity.ok(payments);
    }

    @PutMapping("/{paymentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDTO> updatePaymentStatus(
            @PathVariable String paymentId,
            @RequestParam PaymentStatus status) {

        log.debug("Updating payment: {} to status: {}", paymentId, status);

        PaymentResponseDTO payment = paymentService.updatePaymentStatus(paymentId, status);

        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{paymentId}/cancel")
    @PreAuthorize("@securityService.canAccessPayment(#paymentId)")
    public ResponseEntity<PaymentResponseDTO> cancelPayment(@PathVariable String paymentId) {
        log.debug("Cancelling payment: {}", paymentId);

        PaymentResponseDTO payment = paymentService.cancelPayment(paymentId);

        return ResponseEntity.ok(payment);
    }

    @PostMapping("/refunds")
    @PreAuthorize("@securityService.canAccessPayment(#refundRequest.paymentId)")
    public ResponseEntity<RefundResponseDTO> createRefund(
            @Valid @RequestBody RefundRequestDTO refundRequest) {

        log.debug("Creating refund for payment: {}", refundRequest.getPaymentId());

        RefundResponseDTO refund = refundService.createRefund(refundRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(refund.getRefundId())
                .toUri();

        return ResponseEntity.created(location).body(refund);
    }

    @GetMapping("/refunds/{refundId}")
    @PreAuthorize("@securityService.canAccessRefund(#refundId)")
    public ResponseEntity<RefundResponseDTO> getRefund(@PathVariable String refundId) {
        log.debug("Getting refund: {}", refundId);

        RefundResponseDTO refund = refundService.getRefund(refundId);

        return ResponseEntity.ok(refund);
    }

    @GetMapping("/{paymentId}/refunds")
    @PreAuthorize("@securityService.canAccessPayment(#paymentId)")
    public ResponseEntity<Page<RefundResponseDTO>> getRefundsByPayment(
            @PathVariable String paymentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.debug("Getting refunds for payment: {}", paymentId);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<RefundResponseDTO> refunds = refundService.getRefundsByPayment(paymentId, pageable);

        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/{paymentId}/available-refund")
    @PreAuthorize("@securityService.canAccessPayment(#paymentId)")
    public ResponseEntity<BigDecimal> getAvailableRefundAmount(@PathVariable String paymentId) {
        log.debug("Getting available refund amount for payment: {}", paymentId);

        BigDecimal amount = refundService.getAvailableRefundAmount(paymentId);

        return ResponseEntity.ok(amount);
    }

    // Public endpoint for checkout success/cancel redirects
    @GetMapping("/public/status/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPublicPaymentStatus(@PathVariable String paymentId) {
        log.debug("Getting public payment status for: {}", paymentId);

        // Only return minimal payment information (status, paymentId, orderId)
        PaymentResponseDTO payment = paymentService.getPayment(paymentId);

        // Create a new DTO with limited information
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setPaymentId(payment.getPaymentId());
        response.setOrderId(payment.getOrderId());
        response.setStatus(payment.getStatus());

        return ResponseEntity.ok(response);
    }
}
