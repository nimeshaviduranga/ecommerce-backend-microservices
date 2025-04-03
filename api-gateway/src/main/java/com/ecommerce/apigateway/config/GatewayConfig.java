package com.ecommerce.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Configuration
public class GatewayConfig {

    // Key resolver for rate limiting based on user IP
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just(clientIp);
        };
    }

    // Fallback routes for circuit breakers
    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        return RouterFunctions
                .route(RequestPredicates.path("/fallback/auth"),
                        request -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("message", "Auth Service is not available")))
                .andRoute(RequestPredicates.path("/fallback/user"),
                        request -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("message", "User Service is not available")))
                .andRoute(RequestPredicates.path("/fallback/product"),
                        request -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("message", "Product Service is not available")))
                .andRoute(RequestPredicates.path("/fallback/order"),
                        request -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("message", "Order Service is not available")))
                .andRoute(RequestPredicates.path("/fallback/cart"),
                        request -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("message", "Cart Service is not available")))
                .andRoute(RequestPredicates.path("/fallback/payment"),
                        request -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("message", "Payment Service is not available")));
    }
}
