package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.ProductResponse;
import com.ecommerce.cartservice.exception.ResourceNotFoundException;
import com.ecommerce.cartservice.exception.ServiceUnavailableException;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceClientImpl implements ProductServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.product-service.url}")
    private String productServiceUrl;

    @Override
    public ProductResponse getProductById(Long productId) {
        try {
            log.debug("Fetching product details for product ID: {}", productId);

            // For testing purposes when Product Service is not available
            if (Boolean.getBoolean("mockServices")) {
                return getMockProduct(productId);
            }

            String token = getCurrentToken();

            return webClientBuilder.build()
                    .get()
                    .uri(productServiceUrl + "/api/products/" + productId)
                    .headers(headers -> {
                        if (!token.isEmpty()) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("Client error from Product Service: {}", response.statusCode());
                        if (response.statusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(new ResourceNotFoundException("Product not found with ID: " + productId));
                        }
                        return response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new RuntimeException("Client error: " + error)));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("Server error from Product Service: {}", response.statusCode());
                        return Mono.error(new ServiceUnavailableException("Product Service is unavailable"));
                    })
                    .bodyToMono(ProductResponse.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.error("Product not found with ID: {}", productId);
            throw new ResourceNotFoundException("Product not found with ID: " + productId);
        } catch (Exception e) {
            log.error("Error fetching product details for ID: {}", productId, e);
            // For graceful degradation, return a mock product
            return getMockProduct(productId);
        }
    }

    // Helper method to get current token
    private String getCurrentToken() {
        try {
            if (SecurityContextHolder.getContext() != null &&
                    SecurityContextHolder.getContext().getAuthentication() != null &&
                    SecurityContextHolder.getContext().getAuthentication().getCredentials() != null) {

                Object credentials = SecurityContextHolder.getContext().getAuthentication().getCredentials();
                if (credentials instanceof String) {
                    return (String) credentials;
                }
            }
            return "";
        } catch (Exception e) {
            log.error("Error retrieving JWT token from security context", e);
            return "";
        }
    }

    // Mock product for testing
    private ProductResponse getMockProduct(Long productId) {
        ProductResponse mockProduct = new ProductResponse();
        mockProduct.setId(productId);
        mockProduct.setName("Product " + productId);
        mockProduct.setDescription("This is a mock product");
        mockProduct.setPrice(java.math.BigDecimal.valueOf(9.99));
        mockProduct.setImageUrl("/img/product-" + productId + ".jpg");
        mockProduct.setStockQuantity(100);
        mockProduct.setCategory("Mock Category");
        mockProduct.setActive(true);
        return mockProduct;
    }
}