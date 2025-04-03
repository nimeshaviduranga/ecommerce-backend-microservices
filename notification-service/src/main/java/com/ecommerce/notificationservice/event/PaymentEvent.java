package com.ecommerce.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;

    private Long paymentId;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String email;
    private String phoneNumber;
    private String status;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionId;
}