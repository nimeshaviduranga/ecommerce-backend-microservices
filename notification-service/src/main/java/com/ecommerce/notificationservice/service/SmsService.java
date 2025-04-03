package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.SmsNotification;

import java.util.Map;

public interface SmsService {

    boolean sendSms(SmsNotification smsNotification);

    boolean sendSimpleSms(String phoneNumber, String message);

    boolean sendTemplatedSms(String phoneNumber, String templateName, Map<String, Object> templateData);
}