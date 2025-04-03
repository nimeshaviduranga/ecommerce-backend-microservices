package com.ecommerce.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    private String notes;

    @NotNull(message = "Shipping address is required")
    @Valid
    private OrderAddressDTO shippingAddress;

    @Valid
    private OrderAddressDTO billingAddress;

    // In a real system, we might include payment info here,
    // but for simplicity we'll handle that separately
}