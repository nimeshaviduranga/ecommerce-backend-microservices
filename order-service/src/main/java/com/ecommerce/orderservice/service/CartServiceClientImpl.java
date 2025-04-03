package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CartResponse;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceClientImpl implements CartServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.cart-service.url}")
    private String cartServiceUrl;

    @Override
    public CartResponse getCartByUserId(Long userId) {
        try {
            log.debug("Fetching cart for user ID: {}", userId);

            // For testing purposes when Cart Service is not available
            if (Boolean.getBoolean("mockServices")) {
                return getMockCart(userId);
            }

            String token = getCurrentToken();
            log.debug("Using token for cart service call: {}", token.isEmpty() ? "EMPTY" : "PRESENT");

            // Use the direct userId endpoint instead of /me
            return webClientBuilder.build()
                    .get()
                    .uri(cartServiceUrl + "/api/carts/user/" + userId)
                    .headers(headers -> {
                        if (!token.isEmpty()) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("Client error from Cart Service: {}", response.statusCode());
                        if (response.statusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(new ResourceNotFoundException("Cart not found for user with ID: " + userId));
                        }
                        return response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new RuntimeException("Client error: " + error)));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("Server error from Cart Service: {}", response.statusCode());
                        return Mono.error(new RuntimeException("Cart Service is unavailable"));
                    })
                    .bodyToMono(CartResponse.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.error("Cart not found for user ID: {}", userId);
            throw new ResourceNotFoundException("Cart not found for user with ID: " + userId);
        } catch (Exception e) {
            log.error("Error fetching cart for user ID: {}", userId, e);
            // For graceful degradation, return a mock cart
            return getMockCart(userId);
        }
    }

    @Override
    public void clearCart(Long userId) {
        try {
            log.debug("Clearing cart for user ID: {}", userId);

            // Skip for testing if mock mode is enabled
            if (Boolean.getBoolean("mockServices")) {
                log.debug("Mock mode enabled, skipping clear cart operation");
                return;
            }

            String token = getCurrentToken();

            // Use the direct userId endpoint for clearing
            webClientBuilder.build()
                    .delete()
                    .uri(cartServiceUrl + "/api/carts/user/" + userId + "/clear")
                    .headers(headers -> {
                        if (!token.isEmpty()) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.error("Error clearing cart for user ID: {}", userId, e);
            // Don't throw exception here - non-critical operation
        }
    }

    // Improved token retrieval method
    private String getCurrentToken() {
        try {
            // Extract the Authorization header from the current request's security context
            if (SecurityContextHolder.getContext() != null &&
                    SecurityContextHolder.getContext().getAuthentication() != null &&
                    SecurityContextHolder.getContext().getAuthentication().getCredentials() != null) {

                Object credentials = SecurityContextHolder.getContext().getAuthentication().getCredentials();
                if (credentials instanceof String) {
                    return (String) credentials;
                }

                // If credentials is not a string, check details
                log.debug("Credentials type: {}", credentials.getClass().getName());
            } else {
                log.warn("Security context or authentication is null");
            }

            // Default fallback - note this might not solve all cases
            return "";
        } catch (Exception e) {
            log.error("Error retrieving JWT token from security context", e);
            return "";
        }
    }

    // Mock data for testing when Cart Service is unavailable
    private CartResponse getMockCart(Long userId) {
        log.debug("Creating mock cart for user ID: {}", userId);
        CartResponse mockCart = new CartResponse();
        mockCart.setUserId(userId);
        mockCart.setItems(new HashSet<>());
        mockCart.setTotalPrice(BigDecimal.ZERO);
        mockCart.setTotalItems(0);
        return mockCart;
    }
}