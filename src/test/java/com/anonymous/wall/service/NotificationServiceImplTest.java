package com.anonymous.wall.service;

import com.anonymous.wall.notification.inbox.NotificationEntity;
import com.anonymous.wall.notification.inbox.NotificationRepository;
import com.anonymous.wall.notification.inbox.NotificationServiceImpl;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("NotificationServiceImpl Tests")
class NotificationServiceImplTest {

    private NotificationServiceImpl service;
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        service = new NotificationServiceImpl();
        try {
            var field = NotificationServiceImpl.class.getDeclaredField("notificationRepository");
            field.setAccessible(true);
            field.set(service, notificationRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("createNotification")
    class CreateNotificationTests {

        @Test
        @DisplayName("Should save notification when actor and recipient differ")
        void shouldSaveWhenDifferentUsers() {
            UUID recipient = UUID.randomUUID();
            UUID actor = UUID.randomUUID();
            UUID entityId = UUID.randomUUID();

            service.createNotification(recipient, actor, "COMMENT", entityId, "Title", "Actor Name");

            verify(notificationRepository, times(1)).save(any(NotificationEntity.class));
        }

        @Test
        @DisplayName("Should skip self-notification when actor equals recipient")
        void shouldSkipSelfNotification() {
            UUID userId = UUID.randomUUID();

            service.createNotification(userId, userId, "COMMENT", UUID.randomUUID(), null, null);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Saved entity should have correct type and entity ID")
        void savedEntityShouldHaveCorrectFields() {
            UUID recipient = UUID.randomUUID();
            UUID actor = UUID.randomUUID();
            UUID entityId = UUID.randomUUID();

            service.createNotification(recipient, actor, "INTERNSHIP_COMMENT", entityId, "My Internship", "John");

            verify(notificationRepository).save(argThat(entity ->
                    entity.getRecipientUserId().equals(recipient) &&
                    entity.getActorUserId().equals(actor) &&
                    "INTERNSHIP_COMMENT".equals(entity.getType()) &&
                    entity.getEntityId().equals(entityId) &&
                    "My Internship".equals(entity.getEntityTitle()) &&
                    "John".equals(entity.getActorProfileName()) &&
                    !entity.isRead()
            ));
        }
    }

    @Nested
    @DisplayName("getNotifications")
    class GetNotificationsTests {

        @Test
        @DisplayName("Should delegate to repository with correct parameters")
        void shouldDelegateToRepository() {
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 20);
            Page<NotificationEntity> page = Page.empty();

            when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable))
                    .thenReturn(page);

            Page<NotificationEntity> result = service.getNotifications(userId, pageable);

            assertSame(page, result);
            verify(notificationRepository).findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should return unread count from repository")
        void shouldReturnUnreadCount() {
            UUID userId = UUID.randomUUID();
            when(notificationRepository.countByRecipientUserIdAndRead(userId, false)).thenReturn(5L);

            long count = service.getUnreadCount(userId);

            assertEquals(5L, count);
            verify(notificationRepository).countByRecipientUserIdAndRead(userId, false);
        }

        @Test
        @DisplayName("Should return zero when no unread notifications")
        void shouldReturnZeroWhenNoUnread() {
            UUID userId = UUID.randomUUID();
            when(notificationRepository.countByRecipientUserIdAndRead(userId, false)).thenReturn(0L);

            assertEquals(0L, service.getUnreadCount(userId));
        }
    }

    @Nested
    @DisplayName("markAllRead")
    class MarkAllReadTests {

        @Test
        @DisplayName("Should call updateReadByRecipientUserId with true")
        void shouldMarkAllRead() {
            UUID userId = UUID.randomUUID();

            service.markAllRead(userId);

            verify(notificationRepository).updateReadByRecipientUserId(userId, true);
        }
    }

    @Nested
    @DisplayName("markRead")
    class MarkReadTests {

        @Test
        @DisplayName("Should mark notification as read when user is owner")
        void shouldMarkReadWhenOwner() {
            UUID userId = UUID.randomUUID();
            UUID notificationId = UUID.randomUUID();

            NotificationEntity entity = new NotificationEntity(
                    userId, UUID.randomUUID(), "COMMENT", UUID.randomUUID(), null, null);
            entity.setId(notificationId);

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(entity));

            service.markRead(notificationId, userId);

            verify(notificationRepository).updateReadById(notificationId, true);
        }

        @Test
        @DisplayName("Should throw 404 when notification does not exist")
        void shouldThrow404WhenNotFound() {
            UUID userId = UUID.randomUUID();
            UUID notificationId = UUID.randomUUID();

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            HttpStatusException ex = assertThrows(HttpStatusException.class,
                    () -> service.markRead(notificationId, userId));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        }

        @Test
        @DisplayName("Should throw 403 when notification belongs to different user")
        void shouldThrow403WhenNotOwner() {
            UUID ownerId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID notificationId = UUID.randomUUID();

            NotificationEntity entity = new NotificationEntity(
                    ownerId, UUID.randomUUID(), "COMMENT", UUID.randomUUID(), null, null);
            entity.setId(notificationId);

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(entity));

            HttpStatusException ex = assertThrows(HttpStatusException.class,
                    () -> service.markRead(notificationId, otherUserId));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(notificationRepository, never()).updateReadById(any(), anyBoolean());
        }
    }
}
