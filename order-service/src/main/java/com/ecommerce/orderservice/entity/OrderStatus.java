package com.ecommerce.orderservice.entity;

public enum OrderStatus {
    CREATED,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}