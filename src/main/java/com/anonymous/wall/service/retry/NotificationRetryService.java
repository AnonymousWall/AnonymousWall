package com.anonymous.wall.service.retry;

import com.anonymous.wall.entity.NotificationEntity;
import com.anonymous.wall.service.base.NotificationService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;

import java.util.UUID;

/**
 * Notification retry wrapper.
 */
@Singleton
public class NotificationRetryService {

    private final NotificationService notificationService;

    public NotificationRetryService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Retryable(attempts = "3", delay = "500ms")
    public void createNotification(UUID recipientUserId, UUID actorUserId, String type, UUID entityId,
                                   String entityTitle, String actorProfileName) {
        notificationService.createNotification(recipientUserId, actorUserId, type, entityId, entityTitle, actorProfileName);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public Page<NotificationEntity> getNotifications(UUID userId, Pageable pageable) {
        return notificationService.getNotifications(userId, pageable);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public long getUnreadCount(UUID userId) {
        return notificationService.getUnreadCount(userId);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public void markAllRead(UUID userId) {
        notificationService.markAllRead(userId);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public void markRead(UUID notificationId, UUID userId) {
        notificationService.markRead(notificationId, userId);
    }
}