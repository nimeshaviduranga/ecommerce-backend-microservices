package com.ecommerce.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodDTO {

    private String id;
    private String type;
    private String brand;
    private String last4;
    private String expiryMonth;
    private String expiryYear;
    private boolean isDefault;
}
