package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long id;

    private String orderNumber;

    private Long userId;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private BigDecimal subtotal;

    private BigDecimal tax;

    private BigDecimal shippingCost;

    private String paymentMethod;

    private String paymentStatus;

    private String trackingNumber;

    private String notes;

    private List<OrderItemDTO> items = new ArrayList<>();

    private OrderAddressDTO shippingAddress;

    private OrderAddressDTO billingAddress;

    private List<OrderPaymentDTO> payments = new ArrayList<>();

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}