package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.PushNotification;
import com.ecommerce.notificationservice.exception.NotificationException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationServiceImpl.class);
    private final ResourceLoader resourceLoader;

    @Value("${notification.push.enabled}")
    private boolean pushEnabled;

    @Value("${notification.push.firebase.credentials-file}")
    private String firebaseCredentialsFile;

    @PostConstruct
    private void init() {
        if (pushEnabled) {
            try {
                Resource resource = resourceLoader.getResource(firebaseCredentialsFile);

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    logger.info("Firebase application has been initialized");
                }
            } catch (IOException e) {
                logger.error("Failed to initialize Firebase", e);
                // Don't fail startup, just log the error
            }
        } else {
            logger.info("Push notifications are disabled");
        }
    }

    @Override
    public boolean sendPushNotification(PushNotification pushNotification) {
        logger.debug("Sending push notification: {}", pushNotification.getId());

        if (!pushEnabled) {
            logger.warn("Push notification sending is disabled. Skipping push: {}", pushNotification.getId());
            return false;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(pushNotification.getDeviceToken())
                    .setNotification(Notification.builder()
                            .setTitle(pushNotification.getSubject())
                            .setBody(pushNotification.getContent())
                            .build());

            // Add image if available
            if (pushNotification.getImageUrl() != null && !pushNotification.getImageUrl().isEmpty()) {
                messageBuilder.setNotification(Notification.builder()
                        .setTitle(pushNotification.getSubject())
                        .setBody(pushNotification.getContent())
                        .setImage(pushNotification.getImageUrl())
                        .build());
            }

            // Add action URL if available
            if (pushNotification.getActionUrl() != null && !pushNotification.getActionUrl().isEmpty()) {
                messageBuilder.putData("actionUrl", pushNotification.getActionUrl());
            }

            // Send the message
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            logger.debug("Push notification sent successfully, response: {}", response);
            return true;
        } catch (Exception e) {
            logger.error("Error sending push notification: {}", pushNotification.getId(), e);
            throw new NotificationException("Failed to send push notification: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendSimplePushNotification(String deviceToken, String title, String body) {
        logger.debug("Sending simple push notification to device: {}", deviceToken);

        if (!pushEnabled) {
            logger.warn("Push notification sending is disabled. Skipping push to device: {}", deviceToken);
            return false;
        }

        try {
            Message message = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            // For testing in development/staging environments, we can log instead of sending
            if (isTestEnvironment()) {
                logger.info("TEST MODE - Push notification would be sent to: {}, title: {}, body: {}",
                        deviceToken, title, body);
                return true;
            }

            // Send the message
            String response = FirebaseMessaging.getInstance().send(message);
            logger.debug("Simple push notification sent successfully, response: {}", response);
            return true;
        } catch (FirebaseMessagingException e) {
            logger.error("Error sending simple push notification to device: {}", deviceToken, e);
            throw new NotificationException("Failed to send push notification: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendPushNotificationWithData(String deviceToken, String title, String body, Map<String, String> data) {
        logger.debug("Sending push notification with data to device: {}", deviceToken);

        if (!pushEnabled) {
            logger.warn("Push notification sending is disabled. Skipping push to device: {}", deviceToken);
            return false;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            // Add data payload
            if (data != null && !data.isEmpty()) {
                data.forEach(messageBuilder::putData);
            }

            // Send the message
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            logger.debug("Push notification with data sent successfully, response: {}", response);
            return true;
        } catch (FirebaseMessagingException e) {
            logger.error("Error sending push notification with data to device: {}", deviceToken, e);
            throw new NotificationException("Failed to send push notification with data: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendPushNotificationToMultipleDevices(List<String> deviceTokens, String title, String body) {
        logger.debug("Sending push notification to multiple devices: {}", deviceTokens.size());

        if (!pushEnabled) {
            logger.warn("Push notification sending is disabled. Skipping push to multiple devices");
            return false;
        }

        try {
            if (deviceTokens.isEmpty()) {
                logger.warn("No device tokens provided for multicast push notification");
                return false;
            }

            // Create multicast message
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(deviceTokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            // Send the multicast message
            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
            logger.debug("Multicast push notification sent successfully, success count: {}, failure count: {}",
                    response.getSuccessCount(), response.getFailureCount());

            // Check for failures
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                List<String> failedTokens = new ArrayList<>();

                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        failedTokens.add(deviceTokens.get(i));
                    }
                }

                logger.warn("Failed to send push notifications to tokens: {}", failedTokens);
            }

            // Return true if at least one message was sent successfully
            return response.getSuccessCount() > 0;
        } catch (FirebaseMessagingException e) {
            logger.error("Error sending push notification to multiple devices", e);
            throw new NotificationException("Failed to send multicast push notification: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendPushNotificationToTopic(String topic, String title, String body) {
        logger.debug("Sending push notification to topic: {}", topic);

        if (!pushEnabled) {
            logger.warn("Push notification sending is disabled. Skipping push to topic: {}", topic);
            return false;
        }

        try {
            // Create topic message
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            // Send the message asynchronously
            String response = FirebaseMessaging.getInstance()
                    .sendAsync(message)
                    .get(); // Wait for the response

            logger.debug("Push notification to topic sent successfully, response: {}", response);
            return true;
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error sending push notification to topic: {}", topic, e);
            Thread.currentThread().interrupt();
            throw new NotificationException("Failed to send push notification to topic: " + e.getMessage(), e);
        }
    }

    // Helper methods

    private boolean isTestEnvironment() {
        // Logic to determine if we're in a test/dev environment
        String environment = System.getProperty("spring.profiles.active", "dev");
        return environment.equals("dev") || environment.equals("test");
    }
}