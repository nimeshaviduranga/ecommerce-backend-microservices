package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        log.debug("Received webhook with signature: {}", signature);

        boolean processed = paymentService.processWebhook(payload, signature);

        if (processed) {
            return ResponseEntity.ok("{\"status\": \"success\"}");
        } else {
            return ResponseEntity.badRequest().body("{\"status\": \"error\"}");
        }
    }
}
