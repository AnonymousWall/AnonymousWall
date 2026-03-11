package com.anonymous.wall.service;

import com.anonymous.wall.entity.NotificationEntity;
import com.anonymous.wall.repository.NotificationRepository;
import com.anonymous.wall.service.impl.NotificationServiceImpl;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private NotificationEntity stubNotification(UUID recipientId) {
        NotificationEntity entity = new NotificationEntity(
                recipientId, UUID.randomUUID(), "COMMENT", UUID.randomUUID(), "Post Title", "ActorName");
        entity.setId(UUID.randomUUID());
        return entity;
    }

    private NotificationEntity stubNotificationWithId(UUID id, UUID recipientId) {
        NotificationEntity entity = new NotificationEntity(
                recipientId, UUID.randomUUID(), "COMMENT", UUID.randomUUID(), null, null);
        entity.setId(id);
        return entity;
    }

    // ─── createNotification ────────────────────────────────────────────────────

    @Nested
    @DisplayName("createNotification()")
    class CreateNotificationTests {

        @Test
        @DisplayName("Should save notification when actor and recipient are different users")
        void shouldSaveWhenDifferentUsers() {
            UUID recipient = UUID.randomUUID();
            UUID actor = UUID.randomUUID();

            service.createNotification(recipient, actor, "COMMENT", UUID.randomUUID(), "Title", "Actor");

            verify(notificationRepository, times(1)).save(any(NotificationEntity.class));
        }

        @Test
        @DisplayName("Should skip save when actor equals recipient — no self-notifications")
        void shouldSkipSelfNotification() {
            UUID userId = UUID.randomUUID();

            service.createNotification(userId, userId, "COMMENT", UUID.randomUUID(), null, null);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Saved entity should carry all provided fields and default isRead to false")
        void savedEntityShouldHaveCorrectFields() {
            UUID recipient = UUID.randomUUID();
            UUID actor = UUID.randomUUID();
            UUID entityId = UUID.randomUUID();

            service.createNotification(recipient, actor, "INTERNSHIP_COMMENT", entityId,
                    "My Internship", "John");

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

        @Test
        @DisplayName("Should save successfully when entityTitle and actorProfileName are null")
        void shouldSaveWithNullOptionalFields() {
            UUID recipient = UUID.randomUUID();
            UUID actor = UUID.randomUUID();
            UUID entityId = UUID.randomUUID();

            // entityTitle and actorProfileName are optional — null must not throw
            assertDoesNotThrow(() ->
                    service.createNotification(recipient, actor, "COMMENT", entityId, null, null));

            verify(notificationRepository).save(argThat(entity ->
                    entity.getEntityTitle() == null &&
                            entity.getActorProfileName() == null
            ));
        }

        @Test
        @DisplayName("Should call save exactly once per notification — no duplicate saves")
        void shouldSaveExactlyOnce() {
            UUID recipient = UUID.randomUUID();
            UUID actor = UUID.randomUUID();

            service.createNotification(recipient, actor, "LIKE", UUID.randomUUID(), null, null);

            verify(notificationRepository, times(1)).save(any());
            verifyNoMoreInteractions(notificationRepository);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            UUID recipient = UUID.randomUUID();
            UUID actor = UUID.randomUUID();
            when(notificationRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class, () ->
                    service.createNotification(recipient, actor, "COMMENT",
                            UUID.randomUUID(), null, null));
        }
    }

    // ─── getNotifications ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getNotifications()")
    class GetNotificationsTests {

        @Test
        @DisplayName("Should delegate to repository and return its page")
        void shouldDelegateToRepository() {
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 20);
            Page<NotificationEntity> page = Page.empty();
            when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable))
                    .thenReturn(page);

            Page<NotificationEntity> result = service.getNotifications(userId, pageable);

            assertSame(page, result);
            verify(notificationRepository).findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
            verifyNoMoreInteractions(notificationRepository);
        }

        @Test
        @DisplayName("Should return empty page when user has no notifications")
        void shouldReturnEmptyPageWhenNone() {
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 20);
            when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable))
                    .thenReturn(Page.empty());

            Page<NotificationEntity> result = service.getNotifications(userId, pageable);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should pass userId and pageable to repository unchanged")
        void shouldPassParametersUnchanged() {
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(2, 5);
            when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(any(), any()))
                    .thenReturn(Page.empty());

            service.getNotifications(userId, pageable);

            verify(notificationRepository)
                    .findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            UUID userId = UUID.randomUUID();
            when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(any(), any()))
                    .thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class,
                    () -> service.getNotifications(userId, Pageable.from(0, 20)));
        }
    }

    // ─── getUnreadCount ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should return unread count from repository")
        void shouldReturnUnreadCount() {
            UUID userId = UUID.randomUUID();
            when(notificationRepository.countByRecipientUserIdAndRead(userId, false)).thenReturn(5L);

            assertEquals(5L, service.getUnreadCount(userId));
            verify(notificationRepository).countByRecipientUserIdAndRead(userId, false);
        }

        @Test
        @DisplayName("Should return zero when user has no unread notifications")
        void shouldReturnZeroWhenNoneUnread() {
            UUID userId = UUID.randomUUID();
            when(notificationRepository.countByRecipientUserIdAndRead(userId, false)).thenReturn(0L);

            assertEquals(0L, service.getUnreadCount(userId));
        }

        @Test
        @DisplayName("Should always query with read=false — never queries read notifications")
        void shouldQueryWithReadFalse() {
            UUID userId = UUID.randomUUID();
            when(notificationRepository.countByRecipientUserIdAndRead(any(), anyBoolean()))
                    .thenReturn(3L);

            service.getUnreadCount(userId);

            // Impl must pass `false` — querying read=true would return the wrong count
            verify(notificationRepository).countByRecipientUserIdAndRead(userId, false);
            verify(notificationRepository, never()).countByRecipientUserIdAndRead(userId, true);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            UUID userId = UUID.randomUUID();
            when(notificationRepository.countByRecipientUserIdAndRead(any(), anyBoolean()))
                    .thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class, () -> service.getUnreadCount(userId));
        }
    }

    // ─── markAllRead ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAllRead()")
    class MarkAllReadTests {

        @Test
        @DisplayName("Should call updateReadByRecipientUserId with userId and read=true")
        void shouldMarkAllReadForUser() {
            UUID userId = UUID.randomUUID();

            service.markAllRead(userId);

            verify(notificationRepository).updateReadByRecipientUserId(userId, true);
            verifyNoMoreInteractions(notificationRepository);
        }

        @Test
        @DisplayName("Should always pass read=true — never marks as unread")
        void shouldAlwaysPassTrue() {
            UUID userId = UUID.randomUUID();

            service.markAllRead(userId);

            // Guard against accidental `false` being passed to the bulk update
            verify(notificationRepository, never()).updateReadByRecipientUserId(userId, false);
        }

        @Test
        @DisplayName("Should only update the requesting user's notifications — not other users")
        void shouldScopeToSingleUser() {
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            service.markAllRead(userId);

            verify(notificationRepository).updateReadByRecipientUserId(userId, true);
            verify(notificationRepository, never()).updateReadByRecipientUserId(otherUserId, true);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            UUID userId = UUID.randomUUID();
            doThrow(new RuntimeException("DB error"))
                    .when(notificationRepository).updateReadByRecipientUserId(any(), anyBoolean());

            assertThrows(RuntimeException.class, () -> service.markAllRead(userId));
        }
    }

    // ─── markRead ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markRead()")
    class MarkReadTests {

        @Test
        @DisplayName("Should call updateReadById with notificationId and true when user is owner")
        void shouldMarkReadWhenOwner() {
            UUID userId = UUID.randomUUID();
            UUID notificationId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(stubNotificationWithId(notificationId, userId)));

            service.markRead(notificationId, userId);

            verify(notificationRepository).updateReadById(notificationId, true);
        }

        @Test
        @DisplayName("Should be idempotent — marking an already-read notification does not throw")
        void shouldBeIdempotentForAlreadyReadNotification() {
            UUID userId = UUID.randomUUID();
            UUID notificationId = UUID.randomUUID();
            NotificationEntity alreadyRead = stubNotificationWithId(notificationId, userId);
            alreadyRead.setRead(true);
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(alreadyRead));

            // Impl does not check current read state — it always calls updateReadById.
            // This is intentional: idempotent write is simpler than a conditional update.
            assertDoesNotThrow(() -> service.markRead(notificationId, userId));
            verify(notificationRepository).updateReadById(notificationId, true);
        }

        @Test
        @DisplayName("Should throw 404 when notification does not exist")
        void shouldThrow404WhenNotFound() {
            UUID notificationId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            HttpStatusException ex = assertThrows(HttpStatusException.class,
                    () -> service.markRead(notificationId, UUID.randomUUID()));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            verify(notificationRepository, never()).updateReadById(any(), anyBoolean());
        }

        @Test
        @DisplayName("Should throw 403 when notification belongs to a different user")
        void shouldThrow403WhenNotOwner() {
            UUID ownerId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID notificationId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(stubNotificationWithId(notificationId, ownerId)));

            HttpStatusException ex = assertThrows(HttpStatusException.class,
                    () -> service.markRead(notificationId, otherUserId));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            // updateReadById must never be called when authorization fails
            verify(notificationRepository, never()).updateReadById(any(), anyBoolean());
        }

        @Test
        @DisplayName("Should always pass read=true to updateReadById — never marks as unread")
        void shouldAlwaysPassTrue() {
            UUID userId = UUID.randomUUID();
            UUID notificationId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(stubNotificationWithId(notificationId, userId)));

            service.markRead(notificationId, userId);

            verify(notificationRepository).updateReadById(notificationId, true);
            verify(notificationRepository, never()).updateReadById(notificationId, false);
        }

        @Test
        @DisplayName("Should check ownership before calling updateReadById — ownership is enforced first")
        void shouldCheckOwnershipBeforeUpdating() {
            UUID notificationId = UUID.randomUUID();
            // Ownership check happens before updateReadById; if it doesn't, the
            // `never()` assertion below would pass for the wrong reason.
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            assertThrows(HttpStatusException.class,
                    () -> service.markRead(notificationId, UUID.randomUUID()));

            verify(notificationRepository, never()).updateReadById(any(), anyBoolean());
        }

        @Test
        @DisplayName("Should propagate repository exception from findById")
        void shouldPropagateRepositoryExceptionOnFind() {
            UUID notificationId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId))
                    .thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class,
                    () -> service.markRead(notificationId, UUID.randomUUID()));
        }

        @Test
        @DisplayName("Should propagate repository exception from updateReadById")
        void shouldPropagateRepositoryExceptionOnUpdate() {
            UUID userId = UUID.randomUUID();
            UUID notificationId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(stubNotificationWithId(notificationId, userId)));
            doThrow(new RuntimeException("DB error"))
                    .when(notificationRepository).updateReadById(any(), anyBoolean());

            assertThrows(RuntimeException.class,
                    () -> service.markRead(notificationId, userId));
        }
    }
}
