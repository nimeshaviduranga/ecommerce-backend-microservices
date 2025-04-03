package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequestDTO;
import com.ecommerce.paymentservice.dto.RefundRequestDTO;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.exception.PaymentGatewayException;
import com.ecommerce.paymentservice.exception.PaymentProcessingException;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeServiceImpl implements StripeService {

    private final PaymentRepository paymentRepository;

    @Value("${payment.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${payment.default-currency:USD}")
    private String defaultCurrency;

    @Override
    public PaymentIntent createPaymentIntent(PaymentRequestDTO paymentRequest) {
        try {
            log.debug("Creating payment intent for amount: {}", paymentRequest.getAmount());

            // Convert BigDecimal to cents (Stripe uses the smallest currency unit)
            long amountInCents = convertToCents(paymentRequest.getAmount());

            String currency = paymentRequest.getCurrency() != null
                    ? paymentRequest.getCurrency().toLowerCase()
                    : defaultCurrency.toLowerCase();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("orderId", paymentRequest.getOrderId());

            if (paymentRequest.getMetadata() != null) {
                metadata.put("additionalInfo", String.valueOf(paymentRequest.getMetadata()));
            }

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency)
                    .setDescription("Payment for order " + paymentRequest.getOrderId())
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    );

            // Create the PaymentIntent
            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());
            log.debug("Created payment intent: {}", paymentIntent.getId());

            return paymentIntent;

        } catch (StripeException e) {
            log.error("Error creating payment intent: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to create payment intent: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId) {
        try {
            log.debug("Confirming payment intent: {} with payment method: {}", paymentIntentId, paymentMethodId);

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                    .setPaymentMethod(paymentMethodId)
                    .build();

            PaymentIntent confirmedIntent = paymentIntent.confirm(params);
            log.debug("Confirmed payment intent: {}, status: {}", confirmedIntent.getId(), confirmedIntent.getStatus());

            return confirmedIntent;

        } catch (StripeException e) {
            log.error("Error confirming payment intent: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to confirm payment: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            log.debug("Retrieving payment intent: {}", paymentIntentId);

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            log.debug("Retrieved payment intent: {}, status: {}", paymentIntent.getId(), paymentIntent.getStatus());

            return paymentIntent;

        } catch (StripeException e) {
            log.error("Error retrieving payment intent: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to retrieve payment intent: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) {
        try {
            log.debug("Cancelling payment intent: {}", paymentIntentId);

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent cancelledIntent = paymentIntent.cancel();

            log.debug("Cancelled payment intent: {}, status: {}", cancelledIntent.getId(), cancelledIntent.getStatus());

            return cancelledIntent;

        } catch (StripeException e) {
            log.error("Error cancelling payment intent: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to cancel payment: " + e.getMessage(), e);
        }
    }

    @Override
    public Charge retrieveCharge(String chargeId) {
        try {
            log.debug("Retrieving charge: {}", chargeId);

            Charge charge = Charge.retrieve(chargeId);
            log.debug("Retrieved charge: {}, status: {}", charge.getId(), charge.getStatus());

            return charge;

        } catch (StripeException e) {
            log.error("Error retrieving charge: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to retrieve charge: " + e.getMessage(), e);
        }
    }

    @Override
    public Refund createRefund(String chargeId, BigDecimal amount, String reason, Map<String, String> metadata) {
        try {
            log.debug("Creating refund for charge: {}, amount: {}", chargeId, amount);

            // Convert BigDecimal to cents
            long amountInCents = convertToCents(amount);

            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setCharge(chargeId)
                    .setAmount(amountInCents);

            if (reason != null && !reason.isEmpty()) {
                try {
                    RefundCreateParams.Reason refundReason = RefundCreateParams.Reason.valueOf(reason.toUpperCase());
                    paramsBuilder.setReason(refundReason);
                } catch (IllegalArgumentException e) {
                    // If not a valid enum value, add as metadata
                    if (metadata == null) {
                        metadata = new HashMap<>();
                    }
                    metadata.put("custom_reason", reason);
                }
            }

            if (metadata != null && !metadata.isEmpty()) {
                paramsBuilder.putAllMetadata(metadata);
            }

            Refund refund = Refund.create(paramsBuilder.build());
            log.debug("Created refund: {}, status: {}", refund.getId(), refund.getStatus());

            return refund;

        } catch (StripeException e) {
            log.error("Error creating refund: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to process refund: " + e.getMessage(), e);
        }
    }
    @Override
    public Refund createRefund(RefundRequestDTO refundRequest, String chargeId) {
        Map<String, String> metadata = new HashMap<>();

        if (refundRequest.getMetadata() != null && !refundRequest.getMetadata().isEmpty()) {
            metadata.put("additionalInfo", String.valueOf(refundRequest.getMetadata()));
        }

        return createRefund(
                chargeId,
                refundRequest.getAmount(),
                refundRequest.getReason(),
                metadata
        );
    }

    @Override
    public Refund retrieveRefund(String refundId) {
        try {
            log.debug("Retrieving refund: {}", refundId);

            Refund refund = Refund.retrieve(refundId);
            log.debug("Retrieved refund: {}, status: {}", refund.getId(), refund.getStatus());

            return refund;

        } catch (StripeException e) {
            log.error("Error retrieving refund: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to retrieve refund: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean processWebhookEvent(String payload, String sigHeader) {
        if (sigHeader == null || payload == null) {
            throw new PaymentProcessingException("Missing payload or signature header");
        }

        try {
            log.debug("Processing webhook with signature: {}", sigHeader);

            // Verify the event using the webhook secret
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.debug("Webhook event type: {}", event.getType());

            // Handle specific event types
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;

                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;

                case "charge.refunded":
                    handleChargeRefunded(event);
                    break;

                default:
                    log.debug("Unhandled event type: {}", event.getType());
                    break;
            }

            return true;

        } catch (SignatureVerificationException e) {
            log.error("Invalid signature in webhook: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Invalid webhook signature: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to process webhook: " + e.getMessage(), e);
        }
    }

    // Helper method to update payment status when a payment succeeds
    private void handlePaymentIntentSucceeded(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();

            if (paymentIntent == null) {
                log.error("Failed to deserialize payment intent from webhook");
                return;
            }

            String paymentIntentId = paymentIntent.getId();
            log.debug("Payment succeeded for intent: {}", paymentIntentId);

            // Find the payment in our database
            Optional<Payment> paymentOpt = paymentRepository.findByPaymentIntentId(paymentIntentId);

            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setStatus(PaymentStatus.COMPLETED);

                // Get the first charge (there should be only one for a PaymentIntent)
                if (paymentIntent.getLatestCharge() != null) {
                    String chargeId = paymentIntent.getLatestCharge();
                    try {
                        Charge charge = Charge.retrieve(chargeId);
                        payment.setChargeId(chargeId);
                        payment.setReceiptUrl(charge.getReceiptUrl());
                    } catch (Exception e) {
                        log.error("Error retrieving charge: {}", e.getMessage());
                        // Still set the charge ID even if we couldn't get the receipt URL
                        payment.setChargeId(chargeId);
                    }
                }

                paymentRepository.save(payment);
                log.debug("Updated payment status to COMPLETED for payment ID: {}", payment.getPaymentId());
            } else {
                log.warn("Payment not found for intent ID: {}", paymentIntentId);
            }

        } catch (Exception e) {
            log.error("Error handling payment intent succeeded event: {}", e.getMessage(), e);
        }
    }

    // Helper method to update payment status when a payment fails
    private void handlePaymentIntentFailed(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();

            if (paymentIntent == null) {
                log.error("Failed to deserialize payment intent from webhook");
                return;
            }

            String paymentIntentId = paymentIntent.getId();
            log.debug("Payment failed for intent: {}", paymentIntentId);

            // Find the payment in our database
            Optional<Payment> paymentOpt = paymentRepository.findByPaymentIntentId(paymentIntentId);

            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setStatus(PaymentStatus.FAILED);

                // Get error information if available
                if (paymentIntent.getLastPaymentError() != null) {
                    payment.setErrorCode(paymentIntent.getLastPaymentError().getCode());
                    payment.setErrorMessage(paymentIntent.getLastPaymentError().getMessage());
                }

                paymentRepository.save(payment);
                log.debug("Updated payment status to FAILED for payment ID: {}", payment.getPaymentId());
            } else {
                log.warn("Payment not found for intent ID: {}", paymentIntentId);
            }

        } catch (Exception e) {
            log.error("Error handling payment intent failed event: {}", e.getMessage(), e);
        }
    }

    // Helper method to handle refund events
    private void handleChargeRefunded(Event event) {
        // Implement refund handling logic
        // Similar to the above methods, but update the payment status to REFUNDED
        // or PARTIALLY_REFUNDED based on the refund amount
    }

    // Helper method to convert BigDecimal amount to cents for Stripe
    private long convertToCents(BigDecimal amount) {
        return amount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
