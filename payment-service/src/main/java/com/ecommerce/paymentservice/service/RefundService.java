package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.RefundRequestDTO;
import com.ecommerce.paymentservice.dto.RefundResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface RefundService {

    /**
     * Creates a refund
     *
     * @param refundRequest The refund request
     * @return The refund response
     */
    RefundResponseDTO createRefund(RefundRequestDTO refundRequest);

    /**
     * Gets a refund by ID
     *
     * @param refundId The refund ID
     * @return The refund response
     */
    RefundResponseDTO getRefund(String refundId);

    /**
     * Gets refunds for a payment
     *
     * @param paymentId The payment ID
     * @param pageable Pagination information
     * @return Paged list of refunds
     */
    Page<RefundResponseDTO> getRefundsByPayment(String paymentId, Pageable pageable);

    /**
     * Gets available refund amount for a payment
     *
     * @param paymentId The payment ID
     * @return The available refund amount
     */
    BigDecimal getAvailableRefundAmount(String paymentId);
}
