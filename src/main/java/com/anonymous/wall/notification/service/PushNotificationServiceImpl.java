package com.anonymous.wall.notification.service;

import com.anonymous.wall.notification.apns.ApnsClient;
import com.anonymous.wall.notification.device.DeviceTokenService;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Singleton
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationServiceImpl.class);

    @Inject
    private ApnsClient apnsClient;

    @Inject
    private DeviceTokenService deviceTokenService;

    @Override
    @Retryable(attempts = "2", delay = "2s", excludes = IllegalArgumentException.class)
    public void sendPush(String deviceToken, String title, String body, Map<String, Object> data) {
        int status = apnsClient.send(deviceToken, title, body, data);
        if (status == 200) {
            // Success — no action
        } else if (status == 410) {
            // Permanent failure — token gone, deactivate and do not retry
            deviceTokenService.deactivate(deviceToken);
        } else if (status == 500) {
            // Transient failure — throw so @Retryable can retry
            throw new RuntimeException("APNs transient error for token " + deviceToken + ", status=500");
        } else {
            log.error("APNs error for token {} with status {}", deviceToken, status);
        }
    }
}