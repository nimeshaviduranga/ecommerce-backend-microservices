package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.CartItemRequest;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.security.UserPrincipal;
import com.ecommerce.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping("/me")
    public ResponseEntity<CartResponse> getMyCart(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Getting cart for authenticated user");

        if (principal == null) {
            log.error("User principal is null. This indicates an authentication issue.");
            // Use a default user ID for testing or throw an appropriate exception
            // For now, we'll use a default user ID for development purposes
            Long userId = 1L; // Default test user
            log.debug("Using default user ID: {}", userId);
            return ResponseEntity.ok(cartService.getCartByUserId(userId));
        }

        Long userId = principal.getId();
        log.debug("User ID from principal: {}", userId);
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CartItemRequest request) {

        log.debug("Adding item to cart: {}", request);

        if (principal == null) {
            log.error("User principal is null in addItemToCart");
            Long userId = 1L; // Default test user
            return ResponseEntity.ok(cartService.addItemToCart(userId, request));
        }

        Long userId = principal.getId();
        return ResponseEntity.ok(cartService.addItemToCart(userId, request));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId,
            @RequestParam int quantity) {

        log.debug("Updating item quantity for productId: {} to {}", productId, quantity);

        if (principal == null) {
            log.error("User principal is null in updateCartItem");
            Long userId = 1L; // Default test user
            return ResponseEntity.ok(cartService.updateItemQuantity(userId, productId, quantity));
        }

        Long userId = principal.getId();
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, productId, quantity));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeCartItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId) {

        log.debug("Removing item with productId {} from cart", productId);

        if (principal == null) {
            log.error("User principal is null in removeCartItem");
            Long userId = 1L; // Default test user
            return ResponseEntity.ok(cartService.removeItemFromCart(userId, productId));
        }

        Long userId = principal.getId();
        return ResponseEntity.ok(cartService.removeItemFromCart(userId, productId));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Clearing the cart");

        if (principal == null) {
            log.error("User principal is null in clearCart");
            Long userId = 1L; // Default test user
            cartService.clearCart(userId);
            return ResponseEntity.noContent().build();
        }

        Long userId = principal.getId();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    // Direct user ID endpoints for service-to-service communication

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#userId)")
    public ResponseEntity<CartResponse> getCartByUserId(@PathVariable Long userId) {
        log.debug("Getting cart for user ID: {}", userId);
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @DeleteMapping("/user/{userId}/clear")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#userId)")
    public ResponseEntity<Void> clearCartByUserId(@PathVariable Long userId) {
        log.debug("Clearing cart for user ID: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}