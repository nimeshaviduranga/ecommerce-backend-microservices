package com.ecommerce.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmailNotification extends Notification {

    @Column(nullable = false)
    private String recipientEmail;

    private String ccEmails;

    private String bccEmails;

    @Column(columnDefinition = "TEXT")
    private String htmlContent;

    private boolean hasAttachments = false;

    public EmailNotification(Long userId, String subject, String content, String recipientEmail, String templateName) {
        super();
        this.setUserId(userId);
        this.setSubject(subject);
        this.setContent(content);
        this.setType(NotificationType.EMAIL);
        this.setTemplateName(templateName);
        this.recipientEmail = recipientEmail;
    }

    // Explicit getters for properties used in service
    @Override
    public Long getId() {
        return super.getId();
    }

    public String getHtmlContent() {
        return this.htmlContent;
    }
}