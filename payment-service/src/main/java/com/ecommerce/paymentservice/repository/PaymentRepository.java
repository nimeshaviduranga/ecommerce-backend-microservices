package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByPaymentIntentId(String paymentIntentId);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    Page<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status, Pageable pageable);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime time);
}