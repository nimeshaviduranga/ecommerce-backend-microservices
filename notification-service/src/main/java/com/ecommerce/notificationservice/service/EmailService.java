package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.EmailNotification;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface EmailService {

    boolean sendEmail(EmailNotification emailNotification);

    boolean sendSimpleEmail(String to, String subject, String text);

    boolean sendHtmlEmail(String to, String subject, String htmlContent);

    boolean sendEmailWithAttachments(String to, String subject, String text, List<File> attachments);

    boolean sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> templateData);
}