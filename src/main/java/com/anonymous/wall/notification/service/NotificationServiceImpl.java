package com.anonymous.wall.notification.service;

import com.anonymous.wall.entity.NotificationEntity;
import com.anonymous.wall.model.NotificationDTO;
import com.anonymous.wall.repository.NotificationRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Singleton
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Inject
    private NotificationRepository notificationRepository;

    @Override
    public void createNotification(UUID recipientUserId, UUID actorUserId, String type, UUID entityId,
                                   String entityTitle, String actorProfileName) {
        if (recipientUserId.equals(actorUserId)) {
            return;
        }
        NotificationEntity entity = new NotificationEntity(
                recipientUserId, actorUserId, type, entityId, entityTitle, actorProfileName);
        notificationRepository.save(entity);
        log.debug("Notification created: type={}, recipientUserId={}", type, recipientUserId);
    }

    @Override
    public Page<NotificationDTO> getNotifications(UUID userId, int page, int size) {
        Pageable pageable = Pageable.from(page, size);
        Page<NotificationEntity> entities =
                notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
        return entities.map(this::toDTO);
    }

    @Override
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientUserIdAndRead(userId, false);
    }

    @Override
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadByRecipientUserId(userId);
        log.debug("All notifications marked read for userId={}", userId);
    }

    @Override
    public void markRead(UUID notificationId, UUID userId) {
        NotificationEntity entity = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!entity.getRecipientUserId().equals(userId)) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Notification does not belong to user");
        }
        notificationRepository.markReadById(notificationId);
        log.debug("Notification {} marked read for userId={}", notificationId, userId);
    }

    private NotificationDTO toDTO(NotificationEntity entity) {
        return new NotificationDTO(
                entity.getId(),
                entity.getType(),
                entity.getEntityId(),
                entity.getEntityTitle(),
                entity.getActorProfileName(),
                entity.isRead(),
                entity.getCreatedAt() != null
                        ? entity.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        : null
        );
    }
}
