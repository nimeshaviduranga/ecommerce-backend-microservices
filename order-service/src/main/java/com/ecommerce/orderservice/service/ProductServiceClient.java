package com.ecommerce.orderservice.service;

public interface ProductServiceClient {

    boolean updateProductStock(Long productId, int quantity);
}

