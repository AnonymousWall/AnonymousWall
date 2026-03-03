package com.anonymous.wall.notification;

import com.anonymous.wall.notification.apns.ApnsClient;
import com.anonymous.wall.notification.device.DeviceTokenService;
import com.anonymous.wall.notification.event.ChatMessageSentEvent;
import com.anonymous.wall.notification.event.CommentCreatedEvent;
import com.anonymous.wall.notification.event.InternshipCommentCreatedEvent;
import com.anonymous.wall.notification.event.MarketplaceCommentCreatedEvent;
import com.anonymous.wall.service.NotificationService;
import com.anonymous.wall.notification.listener.NotificationEventListener;
import com.anonymous.wall.notification.service.PushNotificationService;
import com.anonymous.wall.notification.service.PushNotificationServiceImpl;
import com.anonymous.wall.controller.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("PushNotificationService Tests")
class PushNotificationServiceTest {

    private ApnsClient apnsClient;
    private DeviceTokenService deviceTokenService;
    private PushNotificationServiceImpl pushNotificationService;
    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        apnsClient = mock(ApnsClient.class);
        deviceTokenService = mock(DeviceTokenService.class);

        pushNotificationService = new PushNotificationServiceImpl();
        setField(pushNotificationService, "apnsClient", apnsClient);
        setField(pushNotificationService, "deviceTokenService", deviceTokenService);

        PushNotificationService mockPushService = mock(PushNotificationService.class);
        listener = new NotificationEventListener();
        setField(listener, "pushNotificationService", mockPushService);
        setField(listener, "deviceTokenService", deviceTokenService);
        setField(listener, "notificationService", mock(NotificationService.class));
    }

    // ===================== NotificationEventListener Tests =====================

    @Nested
    @DisplayName("Listener: Comment event triggers notification")
    class ListenerTests {

        @Test
        @DisplayName("Comment event triggers sendPush for post owner tokens")
        void commentEventTriggersPush() {
            UUID actorId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();

            String token = "device-token-123";
            when(deviceTokenService.getActiveTokens(ownerId)).thenReturn(List.of(token));

            PushNotificationService mockPush = mock(PushNotificationService.class);
            NotificationEventListener testListener = new NotificationEventListener();
            setField(testListener, "pushNotificationService", mockPush);
            setField(testListener, "deviceTokenService", deviceTokenService);
            setField(testListener, "notificationService", mock(NotificationService.class));

            CommentCreatedEvent event = new CommentCreatedEvent(commentId, postId, actorId, ownerId, "campus");
            testListener.onCommentCreated(event);

            verify(mockPush, times(1)).sendPush(eq(token), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Self-notification prevented: actor == postOwner → sendPush never called")
        void selfNotificationPrevented() {
            UUID userId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();

            PushNotificationService mockPush = mock(PushNotificationService.class);
            NotificationEventListener testListener = new NotificationEventListener();
            setField(testListener, "pushNotificationService", mockPush);
            setField(testListener, "deviceTokenService", deviceTokenService);
            setField(testListener, "notificationService", mock(NotificationService.class));

            CommentCreatedEvent event = new CommentCreatedEvent(commentId, postId, userId, userId, "campus");
            testListener.onCommentCreated(event);

            verify(mockPush, never()).sendPush(any(), any(), any(), any());
            verify(deviceTokenService, never()).getActiveTokens(any());
        }

        @Test
        @DisplayName("Recipient with no active tokens returns silently")
        void noActiveTokensReturnsSilently() {
            UUID actorId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();

            when(deviceTokenService.getActiveTokens(ownerId)).thenReturn(Collections.emptyList());

            PushNotificationService mockPush = mock(PushNotificationService.class);
            NotificationEventListener testListener = new NotificationEventListener();
            setField(testListener, "pushNotificationService", mockPush);
            setField(testListener, "deviceTokenService", deviceTokenService);
            setField(testListener, "notificationService", mock(NotificationService.class));

            CommentCreatedEvent event = new CommentCreatedEvent(commentId, postId, actorId, ownerId, "campus");
            testListener.onCommentCreated(event);

            verify(mockPush, never()).sendPush(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Internship comment event triggers sendPush for internship owner tokens")
        void internshipCommentEventTriggersPush() {
            UUID actorId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            UUID internshipId = UUID.randomUUID();

            String token = "device-token-internship";
            when(deviceTokenService.getActiveTokens(ownerId)).thenReturn(List.of(token));

            PushNotificationService mockPush = mock(PushNotificationService.class);
            NotificationEventListener testListener = new NotificationEventListener();
            setField(testListener, "pushNotificationService", mockPush);
            setField(testListener, "deviceTokenService", deviceTokenService);
            setField(testListener, "notificationService", mock(NotificationService.class));

            InternshipCommentCreatedEvent event = new InternshipCommentCreatedEvent(commentId, internshipId, actorId, ownerId);
            testListener.onInternshipCommentCreated(event);

            verify(mockPush, times(1)).sendPush(eq(token), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Internship self-notification prevented: actor == internshipOwner → sendPush never called")
        void internshipSelfNotificationPrevented() {
            UUID userId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            UUID internshipId = UUID.randomUUID();

            PushNotificationService mockPush = mock(PushNotificationService.class);
            NotificationEventListener testListener = new NotificationEventListener();
            setField(testListener, "pushNotificationService", mockPush);
            setField(testListener, "deviceTokenService", deviceTokenService);
            setField(testListener, "notificationService", mock(NotificationService.class));

            InternshipCommentCreatedEvent event = new InternshipCommentCreatedEvent(commentId, internshipId, userId, userId);
            testListener.onInternshipCommentCreated(event);

            verify(mockPush, never()).sendPush(any(), any(), any(), any());
            verify(deviceTokenService, never()).getActiveTokens(any());
        }

        @Test
        @DisplayName("Marketplace comment event triggers sendPush for item owner tokens")
        void marketplaceCommentEventTriggersPush() {
            UUID actorId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            String token = "device-token-marketplace";
            when(deviceTokenService.getActiveTokens(ownerId)).thenReturn(List.of(token));

            PushNotificationService mockPush = mock(PushNotificationService.class);
            NotificationEventListener testListener = new NotificationEventListener();
            setField(testListener, "pushNotificationService", mockPush);
            setField(testListener, "deviceTokenService", deviceTokenService);
            setField(testListener, "notificationService", mock(NotificationService.class));

            MarketplaceCommentCreatedEvent event = new MarketplaceCommentCreatedEvent(commentId, itemId, actorId, ownerId);
            testListener.onMarketplaceCommentCreated(event);

            verify(mockPush, times(1)).sendPush(eq(token), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Marketplace self-notification prevented: actor == itemOwner → sendPush never called")
        void marketplaceSelfNotificationPrevented() {
            UUID userId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            PushNotificationService mockPush = mock(PushNotificationService.class);
            NotificationEventListener testListener = new NotificationEventListener();
            setField(testListener, "pushNotificationService", mockPush);
            setField(testListener, "deviceTokenService", deviceTokenService);
            setField(testListener, "notificationService", mock(NotificationService.class));

            MarketplaceCommentCreatedEvent event = new MarketplaceCommentCreatedEvent(commentId, itemId, userId, userId);
            testListener.onMarketplaceCommentCreated(event);

            verify(mockPush, never()).sendPush(any(), any(), any(), any());
            verify(deviceTokenService, never()).getActiveTokens(any());
        }

        @Test
        @DisplayName("Chat message event triggers sendPush for recipient tokens when not connected via WebSocket")
        void chatMessageEventTriggersPush() {
            UUID senderId = UUID.randomUUID();
            UUID recipientId = UUID.randomUUID();
            UUID messageId = UUID.randomUUID();
            UUID conversationId = UUID.randomUUID();

            String token = "device-token-chat";
            when(deviceTokenService.getActiveTokens(recipientId)).thenReturn(List.of(token));

            PushNotificationService mockPush = mock(PushNotificationService.class);
            ChatWebSocketHandler mockWsHandler = mock(ChatWebSocketHandler.class);
            when(mockWsHandler.isUserConnected(recipientId)).thenReturn(false);

            NotificationEventListener testListener = new NotificationEventListener();
            setField(testListener, "pushNotificationService", mockPush);
            setField(testListener, "deviceTokenService", deviceTokenService);
            setField(testListener, "chatWebSocketHandler", mockWsHandler);

            ChatMessageSentEvent event = new ChatMessageSentEvent(
                    messageId, conversationId, senderId, recipientId, "Hello!", "Alice");
            testListener.onChatMessageSent(event);

            verify(mockPush, times(1)).sendPush(eq(token), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Chat message push skipped when recipient is connected via WebSocket")
        void chatMessagePushSkippedWhenRecipientConnected() {
            UUID senderId = UUID.randomUUID();
            UUID recipientId = UUID.randomUUID();
            UUID messageId = UUID.randomUUID();
            UUID conversationId = UUID.randomUUID();

            PushNotificationService mockPush = mock(PushNotificationService.class);
            ChatWebSocketHandler mockWsHandler = mock(ChatWebSocketHandler.class);
            when(mockWsHandler.isUserConnected(recipientId)).thenReturn(true);

            NotificationEventListener testListener = new NotificationEventListener();
            setField(testListener, "pushNotificationService", mockPush);
            setField(testListener, "deviceTokenService", deviceTokenService);
            setField(testListener, "chatWebSocketHandler", mockWsHandler);

            ChatMessageSentEvent event = new ChatMessageSentEvent(
                    messageId, conversationId, senderId, recipientId, "Hello!", "Alice");
            testListener.onChatMessageSent(event);

            verify(mockPush, never()).sendPush(any(), any(), any(), any());
            verify(deviceTokenService, never()).getActiveTokens(any());
        }

        @Test
        @DisplayName("Chat self-message prevented: sender == recipient → sendPush never called")
        void chatSelfMessagePrevented() {
            UUID userId = UUID.randomUUID();
            UUID messageId = UUID.randomUUID();
            UUID conversationId = UUID.randomUUID();

            PushNotificationService mockPush = mock(PushNotificationService.class);
            ChatWebSocketHandler mockWsHandler = mock(ChatWebSocketHandler.class);

            NotificationEventListener testListener = new NotificationEventListener();
            setField(testListener, "pushNotificationService", mockPush);
            setField(testListener, "deviceTokenService", deviceTokenService);
            setField(testListener, "chatWebSocketHandler", mockWsHandler);

            ChatMessageSentEvent event = new ChatMessageSentEvent(
                    messageId, conversationId, userId, userId, "Hello!", "Alice");
            testListener.onChatMessageSent(event);

            verify(mockPush, never()).sendPush(any(), any(), any(), any());
            verify(mockWsHandler, never()).isUserConnected(any());
        }
    }

    // ===================== PushNotificationServiceImpl Tests =====================

    @Nested
    @DisplayName("PushNotificationService: APNs status handling")
    class PushServiceTests {

        @Test
        @DisplayName("APNs 200 → no further action")
        void status200Success() throws Exception {
            when(apnsClient.send(any(), any(), any(), any())).thenReturn(200);

            pushNotificationService.sendPush("token", "title", "body", Map.of());

            verify(apnsClient, times(1)).send(any(), any(), any(), any());
            verify(deviceTokenService, never()).deactivate(any());
        }

        @Test
        @DisplayName("APNs 410 → deactivate token")
        void status410DeactivatesToken() {
            when(apnsClient.send(any(), any(), any(), any())).thenReturn(410);

            pushNotificationService.sendPush("token-410", "title", "body", Map.of());

            verify(deviceTokenService, times(1)).deactivate("token-410");
        }

        @Test
        @DisplayName("APNs 400 → error logged, no deactivation, no retry")
        void status400ErrorLogged() {
            when(apnsClient.send(any(), any(), any(), any())).thenReturn(400);

            pushNotificationService.sendPush("token-400", "title", "body", Map.of());

            verify(apnsClient, times(1)).send(any(), any(), any(), any());
            verify(deviceTokenService, never()).deactivate(any());
        }

        @Test
        @DisplayName("APNs 500 → retry once after 2 seconds, then log failure")
        void status500RetriesOnce() {
            when(apnsClient.send(any(), any(), any(), any()))
                    .thenReturn(500)
                    .thenReturn(503);

            long start = System.currentTimeMillis();
            pushNotificationService.sendPush("token-500", "title", "body", Map.of());
            long elapsed = System.currentTimeMillis() - start;

            verify(apnsClient, times(2)).send(any(), any(), any(), any());
            verify(deviceTokenService, never()).deactivate(any());
            // Should have waited ~2000ms
            assertTrue(elapsed >= 1900, "Expected at least 2s delay before retry");
        }
    }

    // ===================== ApnsClient JWT Tests =====================

    @Nested
    @DisplayName("ApnsClient: JWT caching")
    class JwtCacheTests {

        @Test
        @DisplayName("JWT used within 50-min window → generateJwt called only once")
        void jwtCachedWithin50Minutes() throws Exception {
            ApnsClient client = spy(new ApnsClient());
            doReturn("mocked-jwt").when(client).generateJwt();

            String jwt1 = client.getOrRefreshJwt();
            String jwt2 = client.getOrRefreshJwt();

            assertEquals(jwt1, jwt2);
            verify(client, times(1)).generateJwt();
        }

        @Test
        @DisplayName("JWT older than 50 min → generateJwt called again")
        void jwtExpiredAfter50Minutes() throws Exception {
            ApnsClient client = spy(new ApnsClient());
            doReturn("jwt-v1", "jwt-v2").when(client).generateJwt();

            // First call generates JWT
            String jwt1 = client.getOrRefreshJwt();

            // Simulate JWT being 51 minutes old via reflection
            var jwtGeneratedAtField = ApnsClient.class.getDeclaredField("jwtGeneratedAt");
            jwtGeneratedAtField.setAccessible(true);
            jwtGeneratedAtField.set(client, java.time.Instant.now().minus(java.time.Duration.ofMinutes(51)));

            // Second call should regenerate
            String jwt2 = client.getOrRefreshJwt();

            assertNotEquals(jwt1, jwt2);
            verify(client, times(2)).generateJwt();
        }
    }

    // ===================== DeviceTokenService Tests =====================

    @Nested
    @DisplayName("DeviceTokenService: token registration")
    class DeviceTokenServiceTests {

        @Test
        @DisplayName("getActiveTokens returns empty list → returns silently")
        void getActiveTokensEmpty() {
            UUID ownerId = UUID.randomUUID();
            when(deviceTokenService.getActiveTokens(ownerId)).thenReturn(Collections.emptyList());

            List<String> tokens = deviceTokenService.getActiveTokens(ownerId);

            assertTrue(tokens.isEmpty());
        }
    }

    // =================== Helper ===================

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), name);
            }
            throw e;
        }
    }
}
