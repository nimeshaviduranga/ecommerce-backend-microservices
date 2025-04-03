package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderPaymentRequest;
import com.ecommerce.orderservice.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Value("${payment.stripe.enabled:false}")
    private boolean stripeEnabled;

    @Override
    public String processPayment(OrderPaymentRequest paymentRequest) {
        log.debug("Processing payment of {} with method: {}", paymentRequest.getAmount(), paymentRequest.getPaymentMethod());

        // For now, we'll just simulate a payment
        // In a real system, you'd integrate with a payment gateway like Stripe

        // Validate payment method
        validatePaymentMethod(paymentRequest.getPaymentMethod());

        // Process payment (mock implementation)
        String transactionId = generateTransactionId();

        log.debug("Payment processed successfully with transaction ID: {}", transactionId);

        return transactionId;
    }

    @Override
    public boolean refundPayment(String transactionId) {
        log.debug("Refunding payment with transaction ID: {}", transactionId);

        // For now, we'll just simulate a refund
        // In a real system, you'd call the payment gateway's refund API

        log.debug("Refund processed successfully for transaction ID: {}", transactionId);

        return true;
    }

    private void validatePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new BadRequestException("Payment method is required");
        }

        // Validate supported payment methods
        if (!paymentMethod.equals("CREDIT_CARD") &&
                !paymentMethod.equals("PAYPAL") &&
                !paymentMethod.equals("BANK_TRANSFER")) {
            throw new BadRequestException("Unsupported payment method: " + paymentMethod);
        }
    }

    private String generateTransactionId() {
        // Format: TXN-YYYYMMDDHHmmss-XXXX where XXXX is a random alphanumeric string
        return "TXN-" + System.currentTimeMillis() + "-" + RandomStringUtils.randomAlphanumeric(8).toUpperCase();
    }
}