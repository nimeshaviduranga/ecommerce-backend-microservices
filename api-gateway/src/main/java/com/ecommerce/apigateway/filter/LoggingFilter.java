package com.ecommerce.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = UUID.randomUUID().toString();

        // Pre-filter logging
        logger.info("Request {} received: {} {} from {}",
                requestId,
                request.getMethod(),
                request.getPath(),
                request.getRemoteAddress());

        long startTime = System.currentTimeMillis();

        // Add request ID to the outgoing request
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Request-ID", requestId)
                .build();

        // Post-filter execution
        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    long endTime = System.currentTimeMillis();
                    logger.info("Request {} completed in {} ms with status code {}",
                            requestId,
                            endTime - startTime,
                            exchange.getResponse().getStatusCode());
                }));
    }

    @Override
    public int getOrder() {
        // Execute this filter before other filters
        return -1;
    }
}