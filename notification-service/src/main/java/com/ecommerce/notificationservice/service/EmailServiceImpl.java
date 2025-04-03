package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.EmailNotification;
import com.ecommerce.notificationservice.exception.NotificationException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final TemplateService templateService;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.enabled}")
    private boolean emailEnabled;

    @Override
    public boolean sendEmail(EmailNotification emailNotification) {
        logger.debug("Sending email notification: {}", emailNotification.getId());

        if (!emailEnabled) {
            logger.warn("Email sending is disabled. Skipping email: {}", emailNotification.getId());
            return false;
        }

        try {
            String htmlContent = emailNotification.getHtmlContent();
            if (htmlContent != null && !htmlContent.isEmpty()) {
                return sendHtmlEmail(
                        emailNotification.getRecipientEmail(),
                        emailNotification.getSubject(),
                        htmlContent
                );
            } else {
                return sendSimpleEmail(
                        emailNotification.getRecipientEmail(),
                        emailNotification.getSubject(),
                        emailNotification.getContent()
                );
            }
        } catch (Exception e) {
            logger.error("Error sending email notification: {}", emailNotification.getId(), e);
            throw new NotificationException("Failed to send email: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendSimpleEmail(String to, String subject, String text) {
        logger.debug("Sending simple email to: {}", to);

        if (!emailEnabled) {
            logger.warn("Email sending is disabled. Skipping email to: {}", to);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            logger.debug("Simple email sent successfully to: {}", to);
            return true;
        } catch (Exception e) {
            logger.error("Error sending simple email to: {}", to, e);
            throw new NotificationException("Failed to send simple email: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        logger.debug("Sending HTML email to: {}", to);

        if (!emailEnabled) {
            logger.warn("Email sending is disabled. Skipping HTML email to: {}", to);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.debug("HTML email sent successfully to: {}", to);
            return true;
        } catch (MessagingException e) {
            logger.error("Error sending HTML email to: {}", to, e);
            throw new NotificationException("Failed to send HTML email: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendEmailWithAttachments(String to, String subject, String text, List<File> attachments) {
        logger.debug("Sending email with attachments to: {}", to);

        if (!emailEnabled) {
            logger.warn("Email sending is disabled. Skipping email with attachments to: {}", to);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            // Add attachments
            if (attachments != null && !attachments.isEmpty()) {
                for (File file : attachments) {
                    FileSystemResource fileResource = new FileSystemResource(file);
                    helper.addAttachment(file.getName(), fileResource);
                }
            }

            mailSender.send(message);
            logger.debug("Email with attachments sent successfully to: {}", to);
            return true;
        } catch (MessagingException e) {
            logger.error("Error sending email with attachments to: {}", to, e);
            throw new NotificationException("Failed to send email with attachments: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> templateData) {
        logger.debug("Sending templated email to: {} using template: {}", to, templateName);

        if (!emailEnabled) {
            logger.warn("Email sending is disabled. Skipping templated email to: {}", to);
            return false;
        }

        try {
            // Process the template
            String htmlContent = templateService.processEmailTemplate(templateName, templateData);

            // Send the email with the processed template
            return sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            logger.error("Error sending templated email to: {} using template: {}", to, templateName, e);
            throw new NotificationException("Failed to send templated email: " + e.getMessage(), e);
        }
    }
}