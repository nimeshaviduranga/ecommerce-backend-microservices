package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.OrderDTO;
import com.ecommerce.paymentservice.dto.OrderPaymentUpdateRequest;
import com.ecommerce.paymentservice.dto.PaymentResponseDTO;
import com.ecommerce.paymentservice.exception.ResourceNotFoundException;
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
public class OrderServiceClientImpl implements OrderServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.order-service.url}")
    private String orderServiceUrl;

    @Override
    public OrderDTO getOrderById(String orderId) {
        try {
            log.debug("Fetching order details for order ID: {}", orderId);

            String token = getCurrentToken();

            return webClientBuilder.build()
                    .get()
                    .uri(orderServiceUrl + "/api/orders/number/" + orderId)
                    .headers(headers -> {
                        if (!token.isEmpty()) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.error("Client error from Order Service: {}", response.statusCode());
                        if (response.statusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(new ResourceNotFoundException("Order not found with ID: " + orderId));
                        }
                        return response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new RuntimeException("Client error: " + error)));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("Server error from Order Service: {}", response.statusCode());
                        return Mono.error(new RuntimeException("Order Service is unavailable"));
                    })
                    .bodyToMono(OrderDTO.class)
                    .block();

        } catch (WebClientResponseException.NotFound e) {
            log.error("Order not found with ID: {}", orderId);
            throw new ResourceNotFoundException("Order not found with ID: " + orderId);
        } catch (Exception e) {
            log.error("Error fetching order details for ID: {}", orderId, e);
            throw new RuntimeException("Failed to fetch order details: " + e.getMessage());
        }
    }

    @Override
    public boolean updateOrderPaymentStatus(String orderId, PaymentResponseDTO paymentResponse) {
        try {
            log.debug("Updating payment status for order ID: {}", orderId);

            String token = getCurrentToken();

            // Create request object
            OrderPaymentUpdateRequest request = new OrderPaymentUpdateRequest();
            request.setPaymentId(paymentResponse.getPaymentId());
            request.setStatus(mapPaymentStatus(paymentResponse.getStatus().name()));
            request.setTransactionId(paymentResponse.getChargeId());
            request.setAmount(paymentResponse.getAmount());

            webClientBuilder.build()
                    .put()
                    .uri(orderServiceUrl + "/api/orders/" + orderId + "/payment-status")
                    .headers(headers -> {
                        if (!token.isEmpty()) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.debug("Successfully updated payment status for order ID: {}", orderId);
            return true;

        } catch (Exception e) {
            log.error("Error updating payment status for order ID: {}", orderId, e);
            return false;
        }
    }

    // Helper method to get token from security context
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

    // Helper method to map payment status to order payment status
    private String mapPaymentStatus(String paymentStatus) {
        return switch (paymentStatus) {
            case "COMPLETED" -> "PAID";
            case "FAILED" -> "FAILED";
            case "CANCELLED" -> "CANCELLED";
            case "REFUNDED" -> "REFUNDED";
            case "PARTIALLY_REFUNDED" -> "PARTIALLY_REFUNDED";
            default -> "PENDING";
        };
    }
}