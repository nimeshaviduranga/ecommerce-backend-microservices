package com.ecommerce.notificationservice.event.listener;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.event.UserEvent;
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
public class UserEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${kafka.topics.user-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserEvent(UserEvent event) {
        log.debug("Received user event: {} for userId: {}", event.getEventType(), event.getUserId());

        try {
            switch (event.getEventType()) {
                case "USER_CREATED":
                    sendUserCreatedNotifications(event);
                    break;
                case "USER_VERIFIED":
                    sendUserVerifiedNotifications(event);
                    break;
                case "PASSWORD_RESET_REQUESTED":
                    sendPasswordResetNotifications(event);
                    break;
                case "ACCOUNT_LOCKED":
                    sendAccountLockedNotifications(event);
                    break;
                default:
                    log.warn("Unhandled user event type: {}", event.getEventType());
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing user event: {}", event.getEventId(), e);
        }
    }

    private void sendUserCreatedNotifications(UserEvent event) {
        log.debug("Sending user created notifications for user: {}", event.getUserId());

        // Prepare template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", event.getFirstName());
        templateData.put("lastName", event.getLastName());
        templateData.put("username", event.getUsername());

        // Send email welcome notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Welcome to Our E-Commerce Platform!");
            emailRequest.setContent("Thank you for creating an account with us!");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("welcome");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }
    }

    private void sendUserVerifiedNotifications(UserEvent event) {
        log.debug("Sending user verified notifications for user: {}", event.getUserId());

        // Prepare template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", event.getFirstName());

        // Send email notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Your Email Has Been Verified");
            emailRequest.setContent("Your account has been successfully verified!");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("email-verified");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }
    }

    private void sendPasswordResetNotifications(UserEvent event) {
        log.debug("Sending password reset notifications for user: {}", event.getUserId());

        // In a real application, this would include a reset token
        // For demo purposes, we'll use placeholder data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", event.getFirstName());
        templateData.put("resetToken", "DEMO-RESET-TOKEN");
        templateData.put("resetLink", "https://ecommerce.example.com/reset-password?token=DEMO-RESET-TOKEN");
        templateData.put("expiryTime", "1 hour");

        // Send email notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Password Reset Request");
            emailRequest.setContent("You have requested a password reset!");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("password-reset");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }
    }

    private void sendAccountLockedNotifications(UserEvent event) {
        log.debug("Sending account locked notifications for user: {}", event.getUserId());

        // Prepare template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", event.getFirstName());
        templateData.put("contactEmail", "support@ecommerce.example.com");

        // Send email notification
        if (event.getEmail() != null && !event.getEmail().isEmpty()) {
            NotificationRequest emailRequest = new NotificationRequest();
            emailRequest.setUserId(event.getUserId());
            emailRequest.setSubject("Your Account Has Been Locked");
            emailRequest.setContent("Your account has been locked due to multiple failed login attempts.");
            emailRequest.setType(NotificationType.EMAIL);
            emailRequest.setTemplateName("account-locked");
            emailRequest.setRecipientEmail(event.getEmail());

            notificationService.sendNotificationWithTemplate(emailRequest, templateData);
        }
    }
}