package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.OrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {

    List<OrderPayment> findByOrderId(Long orderId);

    Optional<OrderPayment> findByTransactionId(String transactionId);
}