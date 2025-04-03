package com.ecommerce.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "push_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PushNotification extends Notification {

    @Column(nullable = false)
    private String deviceToken;

    private String imageUrl;

    private String actionUrl;

    public PushNotification(Long userId, String subject, String content, String deviceToken, String templateName) {
        super();
        this.setUserId(userId);
        this.setSubject(subject);
        this.setContent(content);
        this.setType(NotificationType.PUSH);
        this.setTemplateName(templateName);
        this.deviceToken = deviceToken;
    }
}