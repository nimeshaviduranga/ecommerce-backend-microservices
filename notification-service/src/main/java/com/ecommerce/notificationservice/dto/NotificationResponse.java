package com.ecommerce.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private boolean success;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();

    public static NotificationResponse success(Long id, String message) {
        return new NotificationResponse(id, true, message, LocalDateTime.now());
    }

    public static NotificationResponse error(String message) {
        return new NotificationResponse(null, false, message, LocalDateTime.now());
    }
}