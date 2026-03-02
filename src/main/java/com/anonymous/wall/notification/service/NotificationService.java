package com.anonymous.wall.notification.service;

import com.anonymous.wall.model.NotificationDTO;
import io.micronaut.data.model.Page;

import java.util.UUID;

public interface NotificationService {
    void createNotification(UUID recipientUserId, UUID actorUserId, String type, UUID entityId,
                            String entityTitle, String actorProfileName);
    Page<NotificationDTO> getNotifications(UUID userId, int page, int size);
    long getUnreadCount(UUID userId);
    void markAllRead(UUID userId);
    void markRead(UUID notificationId, UUID userId);
}
