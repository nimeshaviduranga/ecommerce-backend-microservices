package com.ecommerce.notificationservice.dto;

import com.ecommerce.notificationservice.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    private String templateName;

    // Type-specific fields
    private String recipientEmail; // For EMAIL
    private String ccEmails;       // For EMAIL
    private String bccEmails;      // For EMAIL
    private boolean hasAttachments; // For EMAIL

    private String phoneNumber;    // For SMS

    private String deviceToken;    // For PUSH
    private String imageUrl;       // For PUSH
    private String actionUrl;      // For PUSH

    // Template data (for dynamic content)
    private Map<String, Object> templateData = new HashMap<>();
}