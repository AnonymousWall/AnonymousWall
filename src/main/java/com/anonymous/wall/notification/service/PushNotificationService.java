package com.anonymous.wall.notification.service;

import java.util.Map;

public interface PushNotificationService {
    void sendPush(String deviceToken, String title, String body, Map<String, Object> data);
}
