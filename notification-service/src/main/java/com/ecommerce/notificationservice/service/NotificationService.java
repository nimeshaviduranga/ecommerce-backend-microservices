package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationDTO;
import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    NotificationResponse sendNotification(NotificationRequest request);

    NotificationResponse sendNotificationWithTemplate(NotificationRequest request, Map<String, Object> templateData);

    NotificationDTO getNotificationById(Long id);

    Page<NotificationDTO> getUserNotifications(Long userId, Pageable pageable);

    Page<NotificationDTO> getUserNotificationsByType(Long userId, NotificationType type, Pageable pageable);

    Page<NotificationDTO> getPendingNotifications(Pageable pageable);

    boolean markAsSent(Long id);

    boolean markAsFailed(Long id, String errorMessage);

    List<NotificationDTO> resendFailedNotifications();

    long countUnsentNotifications(Long userId);
}