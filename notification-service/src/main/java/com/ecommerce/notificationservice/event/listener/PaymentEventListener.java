package com.ecommerce.notificationservice.event.listener;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.event.PaymentEvent;
import com.ecommerce.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentEvent(PaymentEvent event) {
        log.debug("Received payment event: {} for paymentId: {}", event.getEventType(), event.getPaymentId());

        try {
            switch (event.getEventType()) {
                case "PAYMENT_RECEIVED":
                    sendPaymentReceivedNotifications(event);
                    break;
                case "PAYMENT_FAILED":
                    sendPaymentFailedNotifications(event);
                    break;
                case "PAYMENT_REFUNDED":
                    sendPaymentRefundedNotifications(event);
                    break;
                default:
                    log.warn("Unhandled payment event type: {}", event.getEventType());
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", event.getEventId(), e);
        }
    }

    private void sendPaymentReceivedNotifications(PaymentEvent event) {
        log.debug("Sending payment received notifications for payment: {}", event.getPaymentId());

        // Prepare template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", event.getOrderNumber());
        templateData.put("paymentDate", event.getTimestamp());
        templateData.put("amount", event.getAmount());
        templateData.put("paymentMethod", event.getPaymentMethod());
        templateData.put("transactionId", event.getTransactionId());

        // Send email notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Payment Received for Order #" + event.getOrderNumber());
            emailRequest.setContent("Your payment has been received!");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("payment-received");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }
    }

    private void sendPaymentFailedNotifications(PaymentEvent event) {
        log.debug("Sending payment failed notifications for payment: {}", event.getPaymentId());

        // Prepare template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", event.getOrderNumber());
        templateData.put("paymentDate", event.getTimestamp());
        templateData.put("amount", event.getAmount());
        templateData.put("paymentMethod", event.getPaymentMethod());

        // Send email notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Payment Failed for Order #" + event.getOrderNumber());
            emailRequest.setContent("Your payment could not be processed!");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("payment-failed");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }
    }

    private void sendPaymentRefundedNotifications(PaymentEvent event) {
        log.debug("Sending payment refunded notifications for payment: {}", event.getPaymentId());

        // Prepare template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", event.getOrderNumber());
        templateData.put("refundDate", event.getTimestamp());
        templateData.put("amount", event.getAmount());
        templateData.put("transactionId", event.getTransactionId());

        // Send email notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Refund Processed for Order #" + event.getOrderNumber());
            emailRequest.setContent("Your refund has been processed!");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("payment-refunded");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }
    }
}