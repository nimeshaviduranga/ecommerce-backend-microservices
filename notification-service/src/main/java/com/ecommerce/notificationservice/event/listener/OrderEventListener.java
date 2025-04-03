package com.ecommerce.notificationservice.event.listener;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.event.OrderEvent;
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
public class OrderEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${kafka.topics.order-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderEvent(OrderEvent event) {
        log.debug("Received order event: {} for orderId: {}", event.getEventType(), event.getOrderId());

        try {
            switch (event.getEventType()) {
                case "ORDER_CREATED":
                    sendOrderCreatedNotifications(event);
                    break;
                case "ORDER_PAID":
                    sendOrderPaidNotifications(event);
                    break;
                case "ORDER_SHIPPED":
                    sendOrderShippedNotifications(event);
                    break;
                case "ORDER_DELIVERED":
                    sendOrderDeliveredNotifications(event);
                    break;
                case "ORDER_CANCELLED":
                    sendOrderCancelledNotifications(event);
                    break;
                default:
                    log.warn("Unhandled order event type: {}", event.getEventType());
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", event.getEventId(), e);
        }
    }

    private void sendOrderCreatedNotifications(OrderEvent event) {
        log.debug("Sending order created notifications for order: {}", event.getOrderNumber());

        // Prepare template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", event.getOrderNumber());
        templateData.put("orderDate", event.getTimestamp());
        templateData.put("customerName", event.getShippingAddress().getFullName());
        templateData.put("orderItems", event.getItems());
        templateData.put("totalAmount", event.getTotalAmount());

        // Send email notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Your Order #" + event.getOrderNumber() + " has been placed");
            emailRequest.setContent("Thank you for your order!");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("order-confirmation");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }

        // Send SMS notification
        if (event.getPhoneNumber() != null && !event.getPhoneNumber().isEmpty()) {
            NotificationRequest smsRequest = new NotificationRequest();
            smsRequest.setUserId(event.getUserId());
            smsRequest.setSubject("Order Confirmation");
            smsRequest.setContent("Thank you for your order!");
            smsRequest.setType(NotificationType.SMS);
            smsRequest.setTemplateName("order-confirmation");
            smsRequest.setPhoneNumber(event.getPhoneNumber());

            notificationService.sendNotificationWithTemplate(smsRequest, templateData);
        }
    }

    private void sendOrderPaidNotifications(OrderEvent event) {
        log.debug("Sending order paid notifications for order: {}", event.getOrderNumber());

        // Prepare template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", event.getOrderNumber());
        templateData.put("orderDate", event.getTimestamp());
        templateData.put("customerName", event.getShippingAddress().getFullName());
        templateData.put("totalAmount", event.getTotalAmount());

        // Send email notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Payment Received for Order #" + event.getOrderNumber());
            emailRequest.setContent("Your payment has been received!");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("payment-confirmation");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }
    }

    private void sendOrderShippedNotifications(OrderEvent event) {
        log.debug("Sending order shipped notifications for order: {}", event.getOrderNumber());

        // Prepare template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", event.getOrderNumber());
        templateData.put("customerName", event.getShippingAddress().getFullName());
        templateData.put("trackingNumber", event.getTrackingNumber());
        templateData.put("shippingAddress", event.getShippingAddress());

        // Send email notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Your Order #" + event.getOrderNumber() + " has been shipped");
            emailRequest.setContent("Your order has been shipped!");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("order-shipped");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }

        // Send SMS notification
        if (event.getPhoneNumber() != null && !event.getPhoneNumber().isEmpty()) {
            NotificationRequest smsRequest = new NotificationRequest();
            smsRequest.setUserId(event.getUserId());
            smsRequest.setSubject("Order Shipped");
            smsRequest.setContent("Your order has been shipped!");
            smsRequest.setType(NotificationType.SMS);
            smsRequest.setTemplateName("order-shipped");
            smsRequest.setPhoneNumber(event.getPhoneNumber());

            notificationService.sendNotificationWithTemplate(smsRequest, templateData);
        }
    }

    private void sendOrderDeliveredNotifications(OrderEvent event) {
        log.debug("Sending order delivered notifications for order: {}", event.getOrderNumber());

        // Prepare template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", event.getOrderNumber());
        templateData.put("customerName", event.getShippingAddress().getFullName());

        // Send email notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Your Order #" + event.getOrderNumber() + " has been delivered");
            emailRequest.setContent("Your order has been delivered!");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("order-delivered");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }
    }

    private void sendOrderCancelledNotifications(OrderEvent event) {
        log.debug("Sending order cancelled notifications for order: {}", event.getOrderNumber());

        // Prepare template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", event.getOrderNumber());
        templateData.put("customerName", event.getShippingAddress().getFullName());

        // Send email notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Your Order #" + event.getOrderNumber() + " has been cancelled");
            emailRequest.setContent("Your order has been cancelled.");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("order-cancelled");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }
    }
}