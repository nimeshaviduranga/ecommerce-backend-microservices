package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.exception.NotificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateServiceImpl implements TemplateService {

    private final TemplateEngine templateEngine;

    @Value("${notification.templates.path}")
    private String templatesBasePath;

    @Override
    public String processEmailTemplate(String templateName, Map<String, Object> templateData) {
        log.debug("Processing email template: {} with data: {}", templateName, templateData);

        try {
            // Create Thymeleaf context and add variables
            Context context = new Context();

            if (templateData != null && !templateData.isEmpty()) {
                templateData.forEach(context::setVariable);
            }

            // Process the template using Thymeleaf
            String processedHtml = templateEngine.process("email/" + templateName, context);

            if (!StringUtils.hasText(processedHtml)) {
                throw new NotificationException("Failed to process email template: " + templateName);
            }

            return processedHtml;
        } catch (Exception e) {
            log.error("Error processing email template: {}", templateName, e);
            throw new NotificationException("Failed to process email template: " + e.getMessage(), e);
        }
    }

    @Override
    public String processTextTemplate(String templateName, Map<String, Object> templateData) {
        log.debug("Processing text template: {} with data: {}", templateName, templateData);

        try {
            // For SMS and other text templates, we use a simpler approach
            String templatePath = getSmsTemplatePath(templateName);
            String templateContent = loadTemplateContent(templatePath);

            // Simple variable replacement
            if (templateData != null && !templateData.isEmpty()) {
                for (Map.Entry<String, Object> entry : templateData.entrySet()) {
                    String placeholder = "${" + entry.getKey() + "}";
                    String value = entry.getValue() != null ? entry.getValue().toString() : "";
                    templateContent = templateContent.replace(placeholder, value);
                }
            }

            return templateContent;
        } catch (Exception e) {
            log.error("Error processing text template: {}", templateName, e);
            throw new NotificationException("Failed to process text template: " + e.getMessage(), e);
        }
    }

    @Override
    public String getEmailTemplatePath(String templateName) {
        if (!templateName.endsWith(".html")) {
            templateName = templateName + ".html";
        }
        return "templates/email/" + templateName;
    }

    @Override
    public String getSmsTemplatePath(String templateName) {
        if (!templateName.endsWith(".txt")) {
            templateName = templateName + ".txt";
        }
        return "templates/sms/" + templateName;
    }

    // Helper method to load template content from file
    private String loadTemplateContent(String templatePath) throws IOException {
        // For classpath resources, use appropriate Spring utilities
        Path path = Paths.get(templatePath);
        if (Files.exists(path)) {
            return Files.readString(path);
        } else {
            // Try to load from classpath
            try {
                java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);
                if (is != null) {
                    return new String(is.readAllBytes());
                }
            } catch (Exception e) {
                log.error("Error loading template from classpath: {}", templatePath, e);
            }
        }

        throw new NotificationException("Template not found: " + templatePath);
    }
}