package com.ecommerce.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentRequest {

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String cardNumber;

    private String cardExpiryMonth;

    private String cardExpiryYear;

    private String cardCvc;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    // Stripe-specific fields for if we integrate with Stripe
    private String stripeToken;
}