package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationDTO;
import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.*;
import com.ecommerce.notificationservice.exception.NotificationException;
import com.ecommerce.notificationservice.exception.ResourceNotFoundException;
import com.ecommerce.notificationservice.repository.EmailNotificationRepository;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import com.ecommerce.notificationservice.repository.PushNotificationRepository;
import com.ecommerce.notificationservice.repository.SmsNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationRepository emailNotificationRepository;
    private final SmsNotificationRepository smsNotificationRepository;
    private final PushNotificationRepository pushNotificationRepository;

    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushNotificationService;
    private final TemplateService templateService;

    @Override
    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.debug("Sending notification of type {} to user {}", request.getType(), request.getUserId());

        try {
            Notification notification = createNotificationEntity(request);

            // Save notification to repository first
            notification = notificationRepository.save(notification);

            boolean sent = false;

            // Send notification based on type
            switch (request.getType()) {
                case EMAIL:
                    sent = emailService.sendEmail((EmailNotification) notification);
                    break;
                case SMS:
                    sent = smsService.sendSms((SmsNotification) notification);
                    break;
                case PUSH:
                    sent = pushNotificationService.sendPushNotification((PushNotification) notification);
                    break;
                default:
                    throw new NotificationException("Unsupported notification type: " + request.getType());
            }

            // Update notification status
            if (sent) {
                notification.setSent(true);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);

                return NotificationResponse.success(
                        notification.getId(),
                        "Notification sent successfully"
                );
            } else {
                throw new NotificationException("Failed to send notification");
            }

        } catch (Exception e) {
            log.error("Error sending notification", e);
            return NotificationResponse.error("Error sending notification: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public NotificationResponse sendNotificationWithTemplate(NotificationRequest request, Map<String, Object> templateData) {
        log.debug("Sending templated notification of type {} to user {}", request.getType(), request.getUserId());

        try {
            // Process template
            String content = request.getContent();
            String subject = request.getSubject();

            if (request.getTemplateName() != null && !request.getTemplateName().isEmpty()) {
                // If template name is provided, process template
                if (request.getType() == NotificationType.EMAIL) {
                    // For emails, we need HTML content
                    String processedHtml = templateService.processEmailTemplate(
                            request.getTemplateName(),
                            templateData
                    );
                    request.setContent(processedHtml);
                } else {
                    // For SMS and Push, we need plain text
                    String processedText = templateService.processTextTemplate(
                            request.getTemplateName(),
                            templateData
                    );
                    request.setContent(processedText);
                }

                // Also process subject if needed
                if (templateData.containsKey("subject")) {
                    request.setSubject(templateData.get("subject").toString());
                }
            }

            // Send notification with processed content
            return sendNotification(request);

        } catch (Exception e) {
            log.error("Error sending templated notification", e);
            return NotificationResponse.error("Error sending templated notification: " + e.getMessage());
        }
    }

    @Override
    public NotificationDTO getNotificationById(Long id) {
        log.debug("Getting notification by ID: {}", id);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        return mapToDTO(notification);
    }

    @Override
    public Page<NotificationDTO> getUserNotifications(Long userId, Pageable pageable) {
        log.debug("Getting notifications for user ID: {}", userId);

        Page<Notification> notificationPage = notificationRepository.findByUserId(userId, pageable);
        return notificationPage.map(this::mapToDTO);
    }

    @Override
    public Page<NotificationDTO> getUserNotificationsByType(Long userId, NotificationType type, Pageable pageable) {
        log.debug("Getting notifications for user ID: {} with type: {}", userId, type);

        Page<Notification> notificationPage = notificationRepository.findByUserIdAndType(userId, type, pageable);
        return notificationPage.map(this::mapToDTO);
    }

    @Override
    public Page<NotificationDTO> getPendingNotifications(Pageable pageable) {
        log.debug("Getting pending notifications");

        // Using the repository method that filters by sent status directly
        return notificationRepository.findBySent(false, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional
    public boolean markAsSent(Long id) {
        log.debug("Marking notification as sent: {}", id);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        notification.setSent(true);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);

        return true;
    }

    @Override
    @Transactional
    public boolean markAsFailed(Long id, String errorMessage) {
        log.debug("Marking notification as failed: {}", id);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        notification.setSent(false);
        notification.setErrorMessage(errorMessage);
        notificationRepository.save(notification);

        return true;
    }

    @Override
    @Transactional
    public List<NotificationDTO> resendFailedNotifications() {
        log.debug("Resending failed notifications");

        // Get notifications that failed or were not sent and are older than 5 minutes
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Notification> failedNotifications = notificationRepository.findBySentAndCreatedAtBefore(false, fiveMinutesAgo);

        log.debug("Found {} failed notifications to resend", failedNotifications.size());

        for (Notification notification : failedNotifications) {
            try {
                boolean sent = false;

                // Resend based on type
                switch (notification.getType()) {
                    case EMAIL:
                        sent = emailService.sendEmail((EmailNotification) notification);
                        break;
                    case SMS:
                        sent = smsService.sendSms((SmsNotification) notification);
                        break;
                    case PUSH:
                        sent = pushNotificationService.sendPushNotification((PushNotification) notification);
                        break;
                }

                // Update notification status
                if (sent) {
                    notification.setSent(true);
                    notification.setSentAt(LocalDateTime.now());
                    notification.setErrorMessage(null);
                }
            } catch (Exception e) {
                log.error("Error resending notification {}", notification.getId(), e);
                notification.setErrorMessage(e.getMessage());
            }

            notificationRepository.save(notification);
        }

        return failedNotifications.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long countUnsentNotifications(Long userId) {
        log.debug("Counting unsent notifications for user ID: {}", userId);

        return notificationRepository.countByUserIdAndSent(userId, false);
    }

    // Helper methods

    private Notification createNotificationEntity(NotificationRequest request) {
        Notification notification;

        switch (request.getType()) {
            case EMAIL:
                validateEmailRequest(request);
                EmailNotification emailNotification = new EmailNotification();
                emailNotification.setRecipientEmail(request.getRecipientEmail());
                emailNotification.setCcEmails(request.getCcEmails());
                emailNotification.setBccEmails(request.getBccEmails());
                emailNotification.setHtmlContent(request.getContent());
                emailNotification.setHasAttachments(request.isHasAttachments());
                notification = emailNotification;
                break;

            case SMS:
                validateSmsRequest(request);
                SmsNotification smsNotification = new SmsNotification();
                smsNotification.setPhoneNumber(request.getPhoneNumber());
                notification = smsNotification;
                break;

            case PUSH:
                validatePushRequest(request);
                PushNotification pushNotification = new PushNotification();
                pushNotification.setDeviceToken(request.getDeviceToken());
                pushNotification.setImageUrl(request.getImageUrl());
                pushNotification.setActionUrl(request.getActionUrl());
                notification = pushNotification;
                break;

            default:
                throw new NotificationException("Unsupported notification type: " + request.getType());
        }

        // Set common fields
        notification.setUserId(request.getUserId());
        notification.setSubject(request.getSubject());
        notification.setContent(request.getContent());
        notification.setType(request.getType());
        notification.setTemplateName(request.getTemplateName());

        return notification;
    }

    private void validateEmailRequest(NotificationRequest request) {
        if (request.getRecipientEmail() == null || request.getRecipientEmail().trim().isEmpty()) {
            throw new NotificationException("Recipient email is required for email notifications");
        }
    }

    private void validateSmsRequest(NotificationRequest request) {
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new NotificationException("Phone number is required for SMS notifications");
        }
    }

    private void validatePushRequest(NotificationRequest request) {
        if (request.getDeviceToken() == null || request.getDeviceToken().trim().isEmpty()) {
            throw new NotificationException("Device token is required for push notifications");
        }
    }

    private NotificationDTO mapToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setSubject(notification.getSubject());
        dto.setContent(notification.getContent());
        dto.setType(notification.getType());
        dto.setTemplateName(notification.getTemplateName());
        dto.setSent(notification.isSent());
        dto.setSentAt(notification.getSentAt());
        dto.setErrorMessage(notification.getErrorMessage());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setUpdatedAt(notification.getUpdatedAt());

        // Set type-specific fields
        if (notification instanceof EmailNotification) {
            EmailNotification emailNotification = (EmailNotification) notification;
            dto.setRecipientEmail(emailNotification.getRecipientEmail());
            dto.setHtmlContent(emailNotification.getHtmlContent());
        } else if (notification instanceof SmsNotification) {
            SmsNotification smsNotification = (SmsNotification) notification;
            dto.setPhoneNumber(smsNotification.getPhoneNumber());
        } else if (notification instanceof PushNotification) {
            PushNotification pushNotification = (PushNotification) notification;
            dto.setDeviceToken(pushNotification.getDeviceToken());
            dto.setImageUrl(pushNotification.getImageUrl());
            dto.setActionUrl(pushNotification.getActionUrl());
        }

        return dto;
    }
}