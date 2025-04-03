package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.CartItemRequest;
import com.ecommerce.cartservice.dto.CartItemResponse;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.ecommerce.cartservice.exception.ResourceNotFoundException;
import com.ecommerce.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartByUserId(Long userId) {
        log.debug("Getting cart for user ID: {}", userId);

        // Find cart or create a new one if not exists
        Cart cart = findOrCreateCart(userId);

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(Long userId, CartItemRequest request) {
        log.debug("Adding item to cart for user ID: {} - Product ID: {}, Quantity: {}",
                userId, request.getProductId(), request.getQuantity());

        // Find cart or create a new one if not exists
        Cart cart = findOrCreateCart(userId);

        // Check if product exists in the cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Update existing item quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            log.debug("Updated existing item quantity to: {}", item.getQuantity());
        } else {
            // Get product details from product service
            var productDetails = productServiceClient.getProductById(request.getProductId());

            // Create new cart item
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setProductName(productDetails.getName());
            newItem.setProductImage(productDetails.getImageUrl());
            newItem.setPrice(productDetails.getPrice());
            newItem.setQuantity(request.getQuantity());

            cart.getItems().add(newItem);
            log.debug("Added new item to cart: {}", newItem);
        }

        // Update cart totals
        updateCartTotals(cart);

        // Save updated cart
        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);

        return mapToCartResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, int quantity) {
        log.debug("Updating item quantity for user ID: {} - Product ID: {}, New Quantity: {}",
                userId, productId, quantity);

        // Validate quantity
        if (quantity <= 0) {
            log.debug("Removing item as quantity is <= 0");
            return removeItemFromCart(userId, productId);
        }

        // Get user's cart
        Cart cart = getCartOrThrow(userId);

        // Find the item in the cart
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item with product ID " + productId + " not found in cart"));

        // Update quantity
        item.setQuantity(quantity);

        // Update cart totals
        updateCartTotals(cart);

        // Save updated cart
        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);

        return mapToCartResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse removeItemFromCart(Long userId, Long productId) {
        log.debug("Removing item from cart for user ID: {} - Product ID: {}", userId, productId);

        // Get user's cart
        Cart cart = getCartOrThrow(userId);

        // Remove the item
        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));

        if (!removed) {
            log.warn("Item with product ID {} not found in cart", productId);
            throw new ResourceNotFoundException("Item with product ID " + productId + " not found in cart");
        }

        // Update cart totals
        updateCartTotals(cart);

        // Save updated cart
        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);

        return mapToCartResponse(savedCart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.debug("Clearing cart for user ID: {}", userId);

        // Find the user's cart
        Optional<Cart> cartOptional = cartRepository.findByUserId(userId);

        if (cartOptional.isPresent()) {
            Cart cart = cartOptional.get();

            // Clear all items
            cart.getItems().clear();

            // Update totals
            updateCartTotals(cart);

            // Save updated cart
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);
        } else {
            log.debug("No cart found for user ID: {}", userId);
        }
    }

    // Helper methods

    private Cart findOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.debug("Creating new cart for user ID: {}", userId);
                    Cart newCart = new Cart();
                    newCart.setId(UUID.randomUUID().toString());
                    newCart.setUserId(userId);
                    newCart.setItems(new HashSet<>());
                    newCart.setTotalPrice(BigDecimal.ZERO);
                    newCart.setTotalItems(0);
                    newCart.setCreatedAt(LocalDateTime.now());
                    newCart.setUpdatedAt(LocalDateTime.now());
                    return cartRepository.save(newCart);
                });
    }

    private Cart getCartOrThrow(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user ID: " + userId));
    }

    private void updateCartTotals(Cart cart) {
        // Calculate total price
        BigDecimal totalPrice = cart.getItems().stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total items
        int totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        cart.setTotalPrice(totalPrice);
        cart.setTotalItems(totalItems);
    }

    private CartResponse mapToCartResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setUserId(cart.getUserId());

        // Map cart items
        response.setItems(cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toSet()));

        response.setTotalPrice(cart.getTotalPrice());
        response.setTotalItems(cart.getTotalItems());
        response.setCreatedAt(cart.getCreatedAt());
        response.setUpdatedAt(cart.getUpdatedAt());

        return response;
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setProductId(item.getProductId());
        response.setProductName(item.getProductName());
        response.setProductImage(item.getProductImage());
        response.setPrice(item.getPrice());
        response.setQuantity(item.getQuantity());

        // Calculate subtotal
        BigDecimal subtotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
        response.setSubtotal(subtotal);

        return response;
    }
}