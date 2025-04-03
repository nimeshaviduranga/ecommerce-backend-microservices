package com.ecommerce.notificationservice.service;

import java.util.Map;

public interface TemplateService {

    String processEmailTemplate(String templateName, Map<String, Object> templateData);

    String processTextTemplate(String templateName, Map<String, Object> templateData);

    String getEmailTemplatePath(String templateName);

    String getSmsTemplatePath(String templateName);
}