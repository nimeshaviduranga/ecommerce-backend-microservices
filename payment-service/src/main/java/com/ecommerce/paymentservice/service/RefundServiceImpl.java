package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.RefundRequestDTO;
import com.ecommerce.paymentservice.dto.RefundResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {

    private final PaymentService paymentService;

    @Override
    public RefundResponseDTO createRefund(RefundRequestDTO refundRequest) {
        return paymentService.createRefund(refundRequest);
    }

    @Override
    public RefundResponseDTO getRefund(String refundId) {
        return paymentService.getRefund(refundId);
    }

    @Override
    public Page<RefundResponseDTO> getRefundsByPayment(String paymentId, Pageable pageable) {
        return paymentService.getRefundsByPayment(paymentId, pageable);
    }

    @Override
    public BigDecimal getAvailableRefundAmount(String paymentId) {
        return paymentService.getAvailableRefundAmount(paymentId);
    }
}