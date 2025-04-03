package com.ecommerce.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentUpdateRequest {

    @NotBlank(message = "Payment ID is required")
    private String paymentId;

    @NotBlank(message = "Status is required")
    private String status;

    private String transactionId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}