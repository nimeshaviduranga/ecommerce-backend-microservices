package com.ecommerce.paymentservice.dto;

import com.ecommerce.paymentservice.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency = "USD";

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Credit card details (not stored in database)
    private String cardNumber;
    private String expiryMonth;
    private String expiryYear;
    private String cvc;

    // PayPal specific fields
    private String paypalEmail;

    // Return URLs
    private String successUrl;
    private String cancelUrl;

    // Additional metadata
    private String metadata;
}