package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.NotificationEntity;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

public interface NotificationService {

    void createNotification(UUID recipientUserId, UUID actorUserId, String type, UUID entityId,
                            String entityTitle, String actorProfileName);

    Page<NotificationEntity> getNotifications(UUID userId, Pageable pageable);

    long getUnreadCount(UUID userId);

    void markAllRead(UUID userId);

    void markRead(UUID notificationId, UUID userId);
}
