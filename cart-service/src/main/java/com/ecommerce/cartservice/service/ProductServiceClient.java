package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.ProductResponse;

/**
 * Client for interacting with the Product Service
 */
public interface ProductServiceClient {

    /**
     * Gets product information by ID
     *
     * @param productId The ID of the product
     * @return The product details
     */
    ProductResponse getProductById(Long productId);
}