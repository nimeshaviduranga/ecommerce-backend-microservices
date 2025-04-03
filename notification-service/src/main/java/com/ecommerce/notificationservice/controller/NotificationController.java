package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.NotificationDTO;
import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.security.UserPrincipal;
import com.ecommerce.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationRequest request) {

        log.debug("Sending notification to user ID: {}", request.getUserId());

        NotificationResponse response = notificationService.sendNotification(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> sendNotificationWithTemplate(
            @Valid @RequestBody NotificationRequest request,
            @RequestBody Map<String, Object> templateData) {

        log.debug("Sending templated notification to user ID: {}", request.getUserId());

        NotificationResponse response = notificationService.sendNotificationWithTemplate(request, templateData);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == @notificationService.getNotificationById(#id).userId")
    public ResponseEntity<NotificationDTO> getNotificationById(@PathVariable Long id) {
        log.debug("Getting notification by ID: {}", id);

        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<NotificationDTO>> getUserNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Long userId = principal.getId();
        log.debug("Getting notifications for user ID: {}", userId);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(notificationService.getUserNotifications(userId, pageable));
    }

    @GetMapping("/me/type/{type}")
    public ResponseEntity<Page<NotificationDTO>> getUserNotificationsByType(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable NotificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Long userId = principal.getId();
        log.debug("Getting notifications for user ID: {} with type: {}", userId, type);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(notificationService.getUserNotificationsByType(userId, type, pageable));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<NotificationDTO>> getPendingNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting pending notifications");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));

        return ResponseEntity.ok(notificationService.getPendingNotifications(pageable));
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> resendNotification(@PathVariable Long id) {
        log.debug("Resending notification with ID: {}", id);

        NotificationDTO notification = notificationService.getNotificationById(id);

        // Create a new request based on the existing notification
        NotificationRequest request = new NotificationRequest();
        request.setUserId(notification.getUserId());
        request.setSubject(notification.getSubject());
        request.setContent(notification.getContent());
        request.setType(notification.getType());
        request.setTemplateName(notification.getTemplateName());

        // Set type-specific fields
        if (notification.getType() == NotificationType.EMAIL) {
            request.setRecipientEmail(notification.getRecipientEmail());
        } else if (notification.getType() == NotificationType.SMS) {
            request.setPhoneNumber(notification.getPhoneNumber());
        } else if (notification.getType() == NotificationType.PUSH) {
            request.setDeviceToken(notification.getDeviceToken());
            request.setImageUrl(notification.getImageUrl());
            request.setActionUrl(notification.getActionUrl());
        }

        // Send the notification
        NotificationResponse response = notificationService.sendNotification(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> retryFailedNotifications() {
        log.debug("Retrying failed notifications");

        int count = notificationService.resendFailedNotifications().size();

        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/unsent")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public ResponseEntity<Long> countUnsentNotifications(
            @RequestParam Long userId) {

        log.debug("Counting unsent notifications for user ID: {}", userId);

        long count = notificationService.countUnsentNotifications(userId);

        return ResponseEntity.ok(count);
    }
}