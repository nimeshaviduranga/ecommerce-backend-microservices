package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.Refund;
import com.ecommerce.paymentservice.entity.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByRefundId(String refundId);

    Optional<Refund> findByRefundIntentId(String refundIntentId);

    Page<Refund> findByPaymentId(Long paymentId, Pageable pageable);

    List<Refund> findByPayment_PaymentId(String paymentId);

    Page<Refund> findByPayment_UserId(Long userId, Pageable pageable);

    List<Refund> findByStatus(RefundStatus status);

    List<Refund> findByStatusAndCreatedAtBefore(RefundStatus status, LocalDateTime time);
}
