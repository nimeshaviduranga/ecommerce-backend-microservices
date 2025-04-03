package com.ecommerce.notificationservice.dto;

import com.ecommerce.notificationservice.entity.NotificationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private Long id;

    private Long userId;

    private String subject;

    private String content;

    private NotificationType type;

    private String templateName;

    private boolean sent;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // Type-specific fields - will be populated based on notification type
    private String recipientEmail; // For EMAIL
    private String htmlContent;    // For EMAIL
    private String phoneNumber;    // For SMS
    private String deviceToken;    // For PUSH
    private String imageUrl;       // For PUSH
    private String actionUrl;      // For PUSH
}