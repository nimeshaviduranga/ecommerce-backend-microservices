package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.PushNotification;

import java.util.List;
import java.util.Map;

public interface PushNotificationService {

    boolean sendPushNotification(PushNotification pushNotification);

    boolean sendSimplePushNotification(String deviceToken, String title, String body);

    boolean sendPushNotificationWithData(String deviceToken, String title, String body, Map<String, String> data);

    boolean sendPushNotificationToMultipleDevices(List<String> deviceTokens, String title, String body);

    boolean sendPushNotificationToTopic(String topic, String title, String body);
}