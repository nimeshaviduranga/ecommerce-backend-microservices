package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.*;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface PaymentService {

    /**
     * Creates a new payment
     *
     * @param userId The user ID
     * @param paymentRequest The payment request
     * @return The payment response
     */
    PaymentResponseDTO createPayment(Long userId, PaymentRequestDTO paymentRequest);

    /**
     * Processes a payment
     *
     * @param paymentId The payment ID
     * @param paymentMethodId The payment method ID (e.g., for Stripe)
     * @return The payment response
     */
    PaymentResponseDTO processPayment(String paymentId, String paymentMethodId);

    /**
     * Gets payment by ID
     *
     * @param paymentId The payment ID
     * @return The payment response
     */
    PaymentResponseDTO getPayment(String paymentId);

    /**
     * Gets payment by order ID
     *
     * @param orderId The order ID
     * @return The payment response
     */
    PaymentResponseDTO getPaymentByOrderId(String orderId);

    /**
     * Gets all payments for a user
     *
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Paged list of payments
     */
    Page<PaymentResponseDTO> getUserPayments(Long userId, Pageable pageable);

    /**
     * Gets payments for a user with a specific status
     *
     * @param userId The user ID
     * @param status The payment status
     * @param pageable Pagination information
     * @return Paged list of payments
     */
    Page<PaymentResponseDTO> getUserPaymentsByStatus(Long userId, PaymentStatus status, Pageable pageable);

    /**
     * Updates payment status
     *
     * @param paymentId The payment ID
     * @param status The new status
     * @return The updated payment
     */
    PaymentResponseDTO updatePaymentStatus(String paymentId, PaymentStatus status);

    /**
     * Cancels a payment
     *
     * @param paymentId The payment ID
     * @return The cancelled payment
     */
    PaymentResponseDTO cancelPayment(String paymentId);

    /**
     * Creates a refund for a payment
     *
     * @param refundRequest The refund request
     * @return The refund response
     */
    RefundResponseDTO createRefund(RefundRequestDTO refundRequest);

    /**
     * Gets a refund by ID
     *
     * @param refundId The refund ID
     * @return The refund
     */
    RefundResponseDTO getRefund(String refundId);

    /**
     * Gets all refunds for a payment
     *
     * @param paymentId The payment ID
     * @return List of refunds
     */
    Page<RefundResponseDTO> getRefundsByPayment(String paymentId, Pageable pageable);

    /**
     * Calculates the available refund amount for a payment
     *
     * @param paymentId The payment ID
     * @return The available refund amount
     */
    BigDecimal getAvailableRefundAmount(String paymentId);

    /**
     * Processes a webhook event from a payment provider
     *
     * @param payload The webhook payload
     * @param signature The webhook signature
     * @return True if processing was successful
     */
    boolean processWebhook(String payload, String signature);
}