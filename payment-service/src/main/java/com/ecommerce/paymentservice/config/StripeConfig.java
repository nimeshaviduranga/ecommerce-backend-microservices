package com.ecommerce.paymentservice.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StripeConfig {

    @Value("${payment.stripe.api-key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        log.info("Initializing Stripe with API key");
        Stripe.apiKey = stripeApiKey;
    }
}