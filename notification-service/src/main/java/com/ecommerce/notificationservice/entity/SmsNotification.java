package com.ecommerce.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sms_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SmsNotification extends Notification {

    @Column(nullable = false)
    private String phoneNumber;

    public SmsNotification(Long userId, String subject, String content, String phoneNumber, String templateName) {
        super();
        this.setUserId(userId);
        this.setSubject(subject);
        this.setContent(content);
        this.setType(NotificationType.SMS);
        this.setTemplateName(templateName);
        this.phoneNumber = phoneNumber;
    }
}