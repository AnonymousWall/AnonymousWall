package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.NotificationEntity;
import com.anonymous.wall.repository.NotificationRepository;
import com.anonymous.wall.service.base.NotificationService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import io.micronaut.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@Singleton
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Inject
    private NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void createNotification(UUID recipientUserId, UUID actorUserId, String type, UUID entityId,
                                   String entityTitle, String actorProfileName) {
        if (recipientUserId.equals(actorUserId)) {
            return;
        }
        NotificationEntity entity = new NotificationEntity(
                recipientUserId, actorUserId, type, entityId, entityTitle, actorProfileName);
        notificationRepository.save(entity);
        log.debug("Notification created: type={}, recipient={}, entity={}", type, recipientUserId, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationEntity> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientUserIdAndRead(userId, false);
    }

    @Override
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.updateReadByRecipientUserId(userId, true);
        log.debug("Marked all notifications as read for user={}", userId);
    }

    @Override
    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        Optional<NotificationEntity> opt = notificationRepository.findById(notificationId);
        if (opt.isEmpty()) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
        NotificationEntity entity = opt.get();
        if (!entity.getRecipientUserId().equals(userId)) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Notification does not belong to user");
        }
        notificationRepository.updateReadById(notificationId, true);
        log.debug("Marked notification={} as read for user={}", notificationId, userId);
    }
}
