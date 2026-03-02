package com.anonymous.wall.notification.service;

import com.anonymous.wall.notification.apns.ApnsClient;
import com.anonymous.wall.notification.device.DeviceTokenService;
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
    public void sendPush(String deviceToken, String title, String body, Map<String, Object> data) {
        try {
            int status = apnsClient.send(deviceToken, title, body, data);
            if (status == 200) {
                // Success — no action
            } else if (status == 410) {
                deviceTokenService.deactivate(deviceToken);
            } else if (status == 500) {
                Thread.sleep(2000);
                int retryStatus = apnsClient.send(deviceToken, title, body, data);
                if (retryStatus != 200) {
                    log.error("APNs retry failed for token {} with status {}", deviceToken, retryStatus);
                }
            } else {
                log.error("APNs error for token {} with status {}", deviceToken, status);
            }
        } catch (Exception e) {
            log.error("APNs send failed for token {}: {}", deviceToken, e.getMessage());
        }
    }
}
