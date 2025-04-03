package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CartResponse;

public interface CartServiceClient {

    /**
     * Gets the cart for a specific user
     *
     * @param userId The ID of the user
     * @return The user's cart data
     */
    CartResponse getCartByUserId(Long userId);

    /**
     * Clears all items from a user's cart
     *
     * @param userId The ID of the user
     */
    void clearCart(Long userId);
}