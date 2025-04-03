package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderPaymentRequest;

public interface PaymentService {

    String processPayment(OrderPaymentRequest paymentRequest);

    boolean refundPayment(String transactionId);
}