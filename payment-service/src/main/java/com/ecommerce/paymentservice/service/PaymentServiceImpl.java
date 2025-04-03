package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.*;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.entity.Refund;
import com.ecommerce.paymentservice.exception.PaymentProcessingException;
import com.ecommerce.paymentservice.exception.ResourceNotFoundException;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.repository.RefundRepository;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final StripeService stripeService;
    private final OrderServiceClient orderServiceClient;

    @Value("${payment.default-currency:USD}")
    private String defaultCurrency;

    @Value("${payment.stripe.success-url}")
    private String successUrl;

    @Value("${payment.stripe.cancel-url}")
    private String cancelUrl;

    @Override
    @Transactional
    public PaymentResponseDTO createPayment(Long userId, PaymentRequestDTO paymentRequest) {
        log.debug("Creating payment for order ID: {}, amount: {}", paymentRequest.getOrderId(), paymentRequest.getAmount());

        // 1. Validate order exists by calling Order Service
        OrderDTO order = orderServiceClient.getOrderById(paymentRequest.getOrderId());

        // 2. Create payment entity
        Payment payment = new Payment();
        payment.setPaymentId(generatePaymentId());
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setUserId(userId);
        payment.setAmount(paymentRequest.getAmount());
        payment.setCurrency(
                paymentRequest.getCurrency() != null ?
                        paymentRequest.getCurrency().toUpperCase() :
                        defaultCurrency.toUpperCase()
        );
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        payment.setMetadata(paymentRequest.getMetadata());

        // 3. Save payment
        Payment savedPayment = paymentRepository.save(payment);
        log.debug("Saved payment with ID: {}", savedPayment.getPaymentId());

        // 4. Create payment intent with Stripe
        try {
            // Set success and cancel URLs if not provided
            if (paymentRequest.getSuccessUrl() == null) {
                paymentRequest.setSuccessUrl(successUrl + "?paymentId=" + savedPayment.getPaymentId());
            }

            if (paymentRequest.getCancelUrl() == null) {
                paymentRequest.setCancelUrl(cancelUrl + "?paymentId=" + savedPayment.getPaymentId());
            }

            PaymentIntent paymentIntent = stripeService.createPaymentIntent(paymentRequest);

            // 5. Update payment with payment intent ID
            savedPayment.setPaymentIntentId(paymentIntent.getId());
            savedPayment = paymentRepository.save(savedPayment);

            // 6. Return response
            PaymentResponseDTO response = mapToPaymentResponse(savedPayment);
            response.setClientSecret(paymentIntent.getClientSecret());

            return response;

        } catch (Exception e) {
            // Update payment status to FAILED
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setErrorMessage(e.getMessage());
            paymentRepository.save(savedPayment);

            throw new PaymentProcessingException("Failed to create payment: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public PaymentResponseDTO processPayment(String paymentId, String paymentMethodId) {
        log.debug("Processing payment ID: {} with payment method: {}", paymentId, paymentMethodId);

        // 1. Find payment
        Payment payment = getPaymentEntity(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentProcessingException("Payment is not in PENDING state");
        }

        try {
            // 2. Update payment status
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            // 3. Confirm payment intent with Stripe
            PaymentIntent paymentIntent = stripeService.confirmPaymentIntent(
                    payment.getPaymentIntentId(),
                    paymentMethodId
            );

            // 4. Handle payment result
            if ("succeeded".equals(paymentIntent.getStatus())) {
                // Payment succeeded
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setProcessedAt(LocalDateTime.now());

                // Get charge information
                if (paymentIntent.getLatestCharge() != null) {
                    String chargeId = paymentIntent.getLatestCharge();
                    Charge charge = null;
                    try {
                        charge = stripeService.retrieveCharge(chargeId);
                        payment.setChargeId(chargeId);
                        payment.setReceiptUrl(charge.getReceiptUrl());
                    } catch (Exception e) {
                        log.error("Error retrieving charge details: {}", e.getMessage());
                        // Still set the charge ID even if we couldn't get the receipt URL
                        payment.setChargeId(chargeId);
                    }
                }

            } else if ("requires_action".equals(paymentIntent.getStatus())) {
                // 3D Secure or other action required
                // No status change, client needs to handle next actions

            } else {
                // Payment failed or other status
                payment.setStatus(PaymentStatus.FAILED);

                if (paymentIntent.getLastPaymentError() != null) {
                    payment.setErrorCode(paymentIntent.getLastPaymentError().getCode());
                    payment.setErrorMessage(paymentIntent.getLastPaymentError().getMessage());
                }
            }

            // 5. Save updated payment
            payment = paymentRepository.save(payment);

            // 6. Update order status if payment is completed
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                PaymentResponseDTO paymentResponse = mapToPaymentResponse(payment);
                orderServiceClient.updateOrderPaymentStatus(payment.getOrderId(), paymentResponse);
            }

            // 7. Return response
            return mapToPaymentResponse(payment);

        } catch (Exception e) {
            // Update payment status to FAILED
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(e.getMessage());
            paymentRepository.save(payment);

            throw new PaymentProcessingException("Failed to process payment: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentResponseDTO getPayment(String paymentId) {
        log.debug("Getting payment by ID: {}", paymentId);
        Payment payment = getPaymentEntity(paymentId);
        return mapToPaymentResponse(payment);
    }

    @Override
    public PaymentResponseDTO getPaymentByOrderId(String orderId) {
        log.debug("Getting payment by order ID: {}", orderId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order ID: " + orderId));
        return mapToPaymentResponse(payment);
    }

    @Override
    public Page<PaymentResponseDTO> getUserPayments(Long userId, Pageable pageable) {
        log.debug("Getting payments for user ID: {}", userId);
        return paymentRepository.findByUserId(userId, pageable)
                .map(this::mapToPaymentResponse);
    }

    @Override
    public Page<PaymentResponseDTO> getUserPaymentsByStatus(Long userId, PaymentStatus status, Pageable pageable) {
        log.debug("Getting payments for user ID: {} with status: {}", userId, status);
        return paymentRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(this::mapToPaymentResponse);
    }

    @Override
    @Transactional
    public PaymentResponseDTO updatePaymentStatus(String paymentId, PaymentStatus status) {
        log.debug("Updating payment ID: {} to status: {}", paymentId, status);

        Payment payment = getPaymentEntity(paymentId);

        // Validate status transition
        validateStatusTransition(payment.getStatus(), status);

        payment.setStatus(status);

        if (status == PaymentStatus.COMPLETED) {
            payment.setProcessedAt(LocalDateTime.now());
        }

        payment = paymentRepository.save(payment);

        // Update order status if payment status changes
        if (status == PaymentStatus.COMPLETED || status == PaymentStatus.FAILED ||
                status == PaymentStatus.CANCELLED || status == PaymentStatus.REFUNDED) {

            PaymentResponseDTO paymentResponse = mapToPaymentResponse(payment);
            orderServiceClient.updateOrderPaymentStatus(payment.getOrderId(), paymentResponse);
        }

        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDTO cancelPayment(String paymentId) {
        log.debug("Cancelling payment ID: {}", paymentId);

        Payment payment = getPaymentEntity(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new PaymentProcessingException("Payment cannot be cancelled in " + payment.getStatus() + " state");
        }

        try {
            // Cancel payment intent with Stripe if exists
            if (payment.getPaymentIntentId() != null) {
                stripeService.cancelPaymentIntent(payment.getPaymentIntentId());
            }

            // Update payment status
            payment.setStatus(PaymentStatus.CANCELLED);
            payment = paymentRepository.save(payment);

            // Update order status
            PaymentResponseDTO paymentResponse = mapToPaymentResponse(payment);
            orderServiceClient.updateOrderPaymentStatus(payment.getOrderId(), paymentResponse);

            return paymentResponse;

        } catch (Exception e) {
            throw new PaymentProcessingException("Failed to cancel payment: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public RefundResponseDTO createRefund(RefundRequestDTO refundRequest) {
        log.debug("Creating refund for payment ID: {}, amount: {}", refundRequest.getPaymentId(), refundRequest.getAmount());

        Payment payment = getPaymentEntity(refundRequest.getPaymentId());

        // Validate payment status
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentProcessingException("Payment must be in COMPLETED state to refund");
        }

        // Validate refund amount
        BigDecimal availableAmount = getAvailableRefundAmount(refundRequest.getPaymentId());
        if (refundRequest.getAmount().compareTo(availableAmount) > 0) {
            throw new PaymentProcessingException("Refund amount exceeds available amount");
        }

        // Create refund entity
        Refund refund = new Refund();
        refund.setRefundId(generateRefundId());
        refund.setPayment(payment);
        refund.setAmount(refundRequest.getAmount());
        refund.setReason(refundRequest.getReason());
        refund.setStatus(com.ecommerce.paymentservice.entity.RefundStatus.PENDING);
        refund.setMetadata(refundRequest.getMetadata());

        // Save refund
        Refund savedRefund = refundRepository.save(refund);
        log.debug("Saved refund with ID: {}", savedRefund.getRefundId());

        try {
            // Process refund with Stripe
            com.stripe.model.Refund stripeRefund = stripeService.createRefund(
                    refundRequest, payment.getChargeId());

            // Update refund with Stripe refund ID
            savedRefund.setRefundIntentId(stripeRefund.getId());
            savedRefund.setStatus(com.ecommerce.paymentservice.entity.RefundStatus.COMPLETED);
            savedRefund.setProcessedAt(LocalDateTime.now());
            savedRefund = refundRepository.save(savedRefund);

            // Update payment status
            if (availableAmount.subtract(refundRequest.getAmount()).compareTo(BigDecimal.ZERO) <= 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }

            payment = paymentRepository.save(payment);

            // Update order status
            PaymentResponseDTO paymentResponse = mapToPaymentResponse(payment);
            orderServiceClient.updateOrderPaymentStatus(payment.getOrderId(), paymentResponse);

            return mapToRefundResponse(savedRefund);

        } catch (Exception e) {
            // Update refund status to FAILED
            savedRefund.setStatus(com.ecommerce.paymentservice.entity.RefundStatus.FAILED);
            savedRefund.setErrorMessage(e.getMessage());
            refundRepository.save(savedRefund);

            throw new PaymentProcessingException("Failed to process refund: " + e.getMessage(), e);
        }
    }

    @Override
    public RefundResponseDTO getRefund(String refundId) {
        log.debug("Getting refund by ID: {}", refundId);
        Refund refund = getRefundEntity(refundId);
        return mapToRefundResponse(refund);
    }

    @Override
    public Page<RefundResponseDTO> getRefundsByPayment(String paymentId, Pageable pageable) {
        log.debug("Getting refunds for payment ID: {}", paymentId);

        Payment payment = getPaymentEntity(paymentId);

        Page<Refund> refunds = refundRepository.findByPaymentId(payment.getId(), pageable);

        return refunds.map(this::mapToRefundResponse);
    }


    @Override
    public BigDecimal getAvailableRefundAmount(String paymentId) {
        log.debug("Calculating available refund amount for payment ID: {}", paymentId);

        Payment payment = getPaymentEntity(paymentId);

        if (payment.getStatus() != PaymentStatus.COMPLETED &&
                payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            return BigDecimal.ZERO;
        }

        // Calculate total refunded amount
        BigDecimal refundedAmount = payment.getRefunds().stream()
                .filter(r -> r.getStatus() == com.ecommerce.paymentservice.entity.RefundStatus.COMPLETED)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate available amount
        return payment.getAmount().subtract(refundedAmount);
    }

    @Override
    public boolean processWebhook(String payload, String signature) {
        log.debug("Processing webhook with signature: {}", signature);
        return stripeService.processWebhookEvent(payload, signature);
    }

    // Helper methods

    private Payment getPaymentEntity(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
    }

    private Refund getRefundEntity(String refundId) {
        return refundRepository.findByRefundId(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found with ID: " + refundId));
    }

    private PaymentResponseDTO mapToPaymentResponse(Payment payment) {
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setPaymentId(payment.getPaymentId());
        response.setOrderId(payment.getOrderId());
        response.setUserId(payment.getUserId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setPaymentIntentId(payment.getPaymentIntentId());
        response.setChargeId(payment.getChargeId());
        response.setReceiptUrl(payment.getReceiptUrl());
        response.setErrorCode(payment.getErrorCode());
        response.setErrorMessage(payment.getErrorMessage());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        response.setProcessedAt(payment.getProcessedAt());

        // Map refunds if needed
        if (payment.getRefunds() != null && !payment.getRefunds().isEmpty()) {
            response.setRefunds(payment.getRefunds().stream()
                    .map(this::mapToRefundResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private RefundResponseDTO mapToRefundResponse(Refund refund) {
        RefundResponseDTO response = new RefundResponseDTO();
        response.setRefundId(refund.getRefundId());
        response.setPaymentId(refund.getPayment().getPaymentId());
        response.setAmount(refund.getAmount());
        response.setStatus(refund.getStatus());
        response.setReason(refund.getReason());
        response.setRefundIntentId(refund.getRefundIntentId());
        response.setErrorCode(refund.getErrorCode());
        response.setErrorMessage(refund.getErrorMessage());
        response.setCreatedAt(refund.getCreatedAt());
        response.setUpdatedAt(refund.getUpdatedAt());
        response.setProcessedAt(refund.getProcessedAt());
        return response;
    }

    private String generatePaymentId() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8) + "-" +
                RandomStringUtils.randomAlphanumeric(4).toUpperCase();
    }

    private String generateRefundId() {
        return "REF-" + UUID.randomUUID().toString().substring(0, 8) + "-" +
                RandomStringUtils.randomAlphanumeric(4).toUpperCase();
    }

    private void validateStatusTransition(PaymentStatus currentStatus, PaymentStatus newStatus) {
        // This is a simplified validation - in a real system, you'd have a more sophisticated state machine
        switch (currentStatus) {
            case PENDING:
                if (newStatus != PaymentStatus.PROCESSING &&
                        newStatus != PaymentStatus.COMPLETED &&
                        newStatus != PaymentStatus.FAILED &&
                        newStatus != PaymentStatus.CANCELLED) {
                    throw new PaymentProcessingException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
                break;
            case PROCESSING:
                if (newStatus != PaymentStatus.COMPLETED &&
                        newStatus != PaymentStatus.FAILED &&
                        newStatus != PaymentStatus.CANCELLED) {
                    throw new PaymentProcessingException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
                break;
            case COMPLETED:
                if (newStatus != PaymentStatus.REFUNDED &&
                        newStatus != PaymentStatus.PARTIALLY_REFUNDED) {
                    throw new PaymentProcessingException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
                break;
            case PARTIALLY_REFUNDED:
                if (newStatus != PaymentStatus.REFUNDED) {
                    throw new PaymentProcessingException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
                break;
            case FAILED:
            case CANCELLED:
            case REFUNDED:
                throw new PaymentProcessingException("Cannot change status from " + currentStatus);
            default:
                throw new PaymentProcessingException("Unknown payment status: " + currentStatus);
        }
    }
}
