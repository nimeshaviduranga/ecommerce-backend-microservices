package com.ecommerce.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long productId;

    private String productName;

    private String productImage;

    private BigDecimal price;

    private int quantity;

    private BigDecimal subtotal;
}