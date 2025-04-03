package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequestDTO;
import com.ecommerce.paymentservice.dto.RefundRequestDTO;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;

import java.math.BigDecimal;
import java.util.Map;

public interface StripeService {

    /**
     * Creates a payment intent with Stripe
     *
     * @param paymentRequest The payment request containing amount and metadata
     * @return The created payment intent
     */
    PaymentIntent createPaymentIntent(PaymentRequestDTO paymentRequest);

    /**
     * Confirms a payment intent with Stripe
     *
     * @param paymentIntentId The payment intent ID to confirm
     * @param paymentMethodId The payment method ID to use
     * @return The confirmed payment intent
     */
    PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId);

    /**
     * Retrieves a payment intent from Stripe
     *
     * @param paymentIntentId The payment intent ID
     * @return The payment intent
     */
    PaymentIntent retrievePaymentIntent(String paymentIntentId);

    /**
     * Cancels a payment intent in Stripe
     *
     * @param paymentIntentId The payment intent ID to cancel
     * @return The cancelled payment intent
     */
    PaymentIntent cancelPaymentIntent(String paymentIntentId);

    /**
     * Retrieves a charge from Stripe
     *
     * @param chargeId The charge ID
     * @return The charge
     */
    Charge retrieveCharge(String chargeId);

    /**
     * Creates a refund in Stripe
     *
     * @param chargeId The charge ID to refund
     * @param amount The amount to refund
     * @param reason The reason for the refund
     * @param metadata Additional metadata
     * @return The created refund
     */
    Refund createRefund(String chargeId, BigDecimal amount, String reason, Map<String, String> metadata);
    /**
     * Creates a refund in Stripe
     *
     * @param refundRequest The refund request containing chargeId, amount, and reason
     * @return The created refund
     */
    Refund createRefund(RefundRequestDTO refundRequest, String chargeId);

    /**
     * Retrieves a refund from Stripe
     *
     * @param refundId The refund ID
     * @return The refund
     */
    Refund retrieveRefund(String refundId);

    /**
     * Validates and processes a webhook event from Stripe
     *
     * @param payload The webhook payload
     * @param sigHeader The signature header
     * @return True if the webhook was processed successfully
     */
    boolean processWebhookEvent(String payload, String sigHeader);
}
