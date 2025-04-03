package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.SmsNotification;
import com.ecommerce.notificationservice.exception.NotificationException;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsServiceImpl implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);
    private final TemplateService templateService;

    @Value("${notification.sms.enabled}")
    private boolean smsEnabled;

    @Value("${notification.sms.twilio.account-sid}")
    private String twilioAccountSid;

    @Value("${notification.sms.twilio.auth-token}")
    private String twilioAuthToken;

    @Value("${notification.sms.twilio.phone-number}")
    private String twilioPhoneNumber;

    @PostConstruct
    private void init() {
        if (smsEnabled) {
            try {
                Twilio.init(twilioAccountSid, twilioAuthToken);
                logger.info("Twilio client initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize Twilio client", e);
                // Don't fail startup, just log the error
            }
        } else {
            logger.info("SMS notifications are disabled");
        }
    }

    @Override
    public boolean sendSms(SmsNotification smsNotification) {
        logger.debug("Sending SMS notification: {}", smsNotification.getId());

        if (!smsEnabled) {
            logger.warn("SMS sending is disabled. Skipping SMS: {}", smsNotification.getId());
            return false;
        }

        try {
            return sendSimpleSms(
                    smsNotification.getPhoneNumber(),
                    smsNotification.getContent()
            );
        } catch (Exception e) {
            logger.error("Error sending SMS notification: {}", smsNotification.getId(), e);
            throw new NotificationException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendSimpleSms(String phoneNumber, String message) {
        logger.debug("Sending simple SMS to: {}", phoneNumber);

        if (!smsEnabled) {
            logger.warn("SMS sending is disabled. Skipping SMS to: {}", phoneNumber);
            return false;
        }

        try {
            // Normalize phone number if needed
            String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);

            // For testing in development/staging environments, we can log instead of sending
            if (isTestEnvironment()) {
                logger.info("TEST MODE - SMS would be sent to: {}, message: {}", normalizedPhoneNumber, message);
                return true;
            }

            // Send SMS via Twilio
            Message twilioMessage = Message.creator(
                            new PhoneNumber(normalizedPhoneNumber),
                            new PhoneNumber(twilioPhoneNumber),
                            message)
                    .create();

            logger.debug("SMS sent successfully to: {}, SID: {}", phoneNumber, twilioMessage.getSid());
            return true;
        } catch (Exception e) {
            logger.error("Error sending simple SMS to: {}", phoneNumber, e);
            throw new NotificationException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendTemplatedSms(String phoneNumber, String templateName, Map<String, Object> templateData) {
        logger.debug("Sending templated SMS to: {} using template: {}", phoneNumber, templateName);

        if (!smsEnabled) {
            logger.warn("SMS sending is disabled. Skipping templated SMS to: {}", phoneNumber);
            return false;
        }

        try {
            // Process the template
            String messageContent = templateService.processTextTemplate(templateName, templateData);

            // Send the SMS with the processed template
            return sendSimpleSms(phoneNumber, messageContent);
        } catch (Exception e) {
            logger.error("Error sending templated SMS to: {} using template: {}", phoneNumber, templateName, e);
            throw new NotificationException("Failed to send templated SMS: " + e.getMessage(), e);
        }
    }

    // Helper methods

    private String normalizePhoneNumber(String phoneNumber) {
        // Simple normalization: ensure number starts with +
        if (phoneNumber.startsWith("+")) {
            return phoneNumber;
        } else if (phoneNumber.startsWith("00")) {
            return "+" + phoneNumber.substring(2);
        } else {
            // Default to US format if no country code
            return "+1" + phoneNumber.replaceAll("[^0-9]", "");
        }
    }

    private boolean isTestEnvironment() {
        // Logic to determine if we're in a test/dev environment
        // This could check for a specific profile, env var, etc.
        String environment = System.getProperty("spring.profiles.active", "dev");
        return environment.equals("dev") || environment.equals("test");
    }
}