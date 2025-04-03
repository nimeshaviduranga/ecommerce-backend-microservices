package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.CartItemRequest;
import com.ecommerce.cartservice.dto.CartResponse;

public interface CartService {

    /**
     * Gets the cart for a specific user
     *
     * @param userId The ID of the user
     * @return The user's cart data
     */
    CartResponse getCartByUserId(Long userId);

    /**
     * Adds an item to the user's cart
     *
     * @param userId The ID of the user
     * @param request The item to add
     * @return The updated cart
     */
    CartResponse addItemToCart(Long userId, CartItemRequest request);

    /**
     * Updates the quantity of an item in the cart
     *
     * @param userId The ID of the user
     * @param productId The ID of the product to update
     * @param quantity The new quantity
     * @return The updated cart
     */
    CartResponse updateItemQuantity(Long userId, Long productId, int quantity);

    /**
     * Removes an item from the cart
     *
     * @param userId The ID of the user
     * @param productId The ID of the product to remove
     * @return The updated cart
     */
    CartResponse removeItemFromCart(Long userId, Long productId);

    /**
     * Clears all items from the cart
     *
     * @param userId The ID of the user
     */
    void clearCart(Long userId);
}