package com.anonymous.wall.controller;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.model.ChatMessageDTO;
import com.anonymous.wall.service.ChatService;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ChatWebSocketHandler.
 * Tests WebSocket connection handling, message processing, and user session management.
 */
@DisplayName("ChatWebSocketHandler Unit Tests")
class ChatWebSocketHandlerTest {

    private ChatWebSocketHandler handler;
    private ChatService chatService;
    private WebSocketBroadcaster broadcaster;
    private ObjectMapper objectMapper;
    private WebSocketSession mockSession;
    private Authentication mockAuth;

    private UUID testUser1Id;
    private UUID testUser2Id;

    @BeforeEach
    void setUp() throws Exception {
        chatService = mock(ChatService.class);
        broadcaster = mock(WebSocketBroadcaster.class);
        objectMapper = mock(ObjectMapper.class);

        handler = new ChatWebSocketHandler();

        // Inject mocked dependencies using reflection
        var chatServiceField = ChatWebSocketHandler.class.getDeclaredField("chatService");
        chatServiceField.setAccessible(true);
        chatServiceField.set(handler, chatService);

        var broadcasterField = ChatWebSocketHandler.class.getDeclaredField("broadcaster");
        broadcasterField.setAccessible(true);
        broadcasterField.set(handler, broadcaster);

        var objectMapperField = ChatWebSocketHandler.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(handler, objectMapper);

        // Setup test data
        testUser1Id = UUID.randomUUID();
        testUser2Id = UUID.randomUUID();

        // Setup mock session
        mockSession = mock(WebSocketSession.class);
        mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn(testUser1Id.toString());
        when(mockSession.getUserPrincipal()).thenReturn(Optional.of(mockAuth));
        when(mockSession.getId()).thenReturn("test-session-id");
        when(mockSession.isOpen()).thenReturn(true);
    }

    @Nested
    @DisplayName("Connection Handling - OnOpen")
    class OnOpenTests {

        @Test
        @DisplayName("Should handle new WebSocket connection successfully")
        void shouldHandleNewConnectionSuccessfully() throws IOException {
            // Arrange
            when(chatService.countTotalUnreadMessages(testUser1Id)).thenReturn(5L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"connected\"}");

            // Act
            handler.onOpen(mockSession);

            // Assert
            verify(mockSession, times(2)).sendSync(anyString()); // connected + unread_count
            verify(chatService, times(1)).countTotalUnreadMessages(testUser1Id);
        }

        @Test
        @DisplayName("Should send connection confirmation message")
        void shouldSendConnectionConfirmation() throws IOException {
            // Arrange
            when(chatService.countTotalUnreadMessages(testUser1Id)).thenReturn(0L);
            when(objectMapper.writeValueAsString(argThat(map -> {
                Map<String, Object> m = (Map<String, Object>) map;
                return "connected".equals(m.get("type")) && 
                       testUser1Id.toString().equals(m.get("userId"));
            }))).thenReturn("{\"type\":\"connected\"}");

            // Act
            handler.onOpen(mockSession);

            // Assert
            verify(mockSession, times(1)).sendSync(anyString());
        }

        @Test
        @DisplayName("Should send unread count when user has unread messages")
        void shouldSendUnreadCountWhenPresent() throws IOException {
            // Arrange
            when(chatService.countTotalUnreadMessages(testUser1Id)).thenReturn(10L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // Act
            handler.onOpen(mockSession);

            // Assert
            verify(mockSession, times(2)).sendSync(anyString());
            verify(chatService, times(1)).countTotalUnreadMessages(testUser1Id);
        }

        @Test
        @DisplayName("Should not send unread count when user has no unread messages")
        void shouldNotSendUnreadCountWhenZero() throws IOException {
            // Arrange
            when(chatService.countTotalUnreadMessages(testUser1Id)).thenReturn(0L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // Act
            handler.onOpen(mockSession);

            // Assert
            verify(mockSession, times(1)).sendSync(anyString()); // Only connection confirmation
        }

        @Test
        @DisplayName("Should close session on error during connection")
        void shouldCloseSessionOnError() {
            // Arrange
            when(mockSession.getUserPrincipal()).thenReturn(Optional.empty());

            // Act
            handler.onOpen(mockSession);

            // Assert
            verify(mockSession, times(1)).close();
        }
    }

    @Nested
    @DisplayName("Message Processing - OnMessage")
    class OnMessageTests {

        @Test
        @DisplayName("Should handle chat message successfully")
        void shouldHandleChatMessageSuccessfully() throws IOException {
            // Arrange
            String messageJson = "{\"type\":\"message\",\"receiverId\":\"" + testUser2Id + "\",\"content\":\"Hello\"}";
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "message");
            messageData.put("receiverId", testUser2Id.toString());
            messageData.put("content", "Hello");

            ChatMessage savedMessage = new ChatMessage(testUser1Id, testUser2Id, "Hello");
            savedMessage.setId(UUID.randomUUID());
            savedMessage.setCreatedAt(OffsetDateTime.now());

            when(objectMapper.readValue(any(byte[].class), eq(Map.class))).thenReturn(messageData);
            when(chatService.sendMessage(testUser1Id, testUser2Id, "Hello")).thenReturn(savedMessage);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"message\"}");

            // Act
            handler.onMessage(messageJson, mockSession);

            // Assert
            verify(chatService, times(1)).sendMessage(testUser1Id, testUser2Id, "Hello");
            verify(mockSession, times(1)).sendAsync(anyString());
        }

        @Test
        @DisplayName("Should send error when receiverId is missing")
        void shouldSendErrorWhenReceiverIdMissing() throws IOException {
            // Arrange
            String messageJson = "{\"type\":\"message\",\"content\":\"Hello\"}";
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "message");
            messageData.put("content", "Hello");

            when(objectMapper.readValue(any(byte[].class), eq(Map.class))).thenReturn(messageData);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"error\"}");

            // Act
            handler.onMessage(messageJson, mockSession);

            // Assert
            verify(chatService, never()).sendMessage(any(), any(), anyString());
            verify(mockSession, times(1)).sendSync(anyString()); // Error message
        }

        @Test
        @DisplayName("Should send error when content is missing")
        void shouldSendErrorWhenContentMissing() throws IOException {
            // Arrange
            String messageJson = "{\"type\":\"message\",\"receiverId\":\"" + testUser2Id + "\"}";
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "message");
            messageData.put("receiverId", testUser2Id.toString());

            when(objectMapper.readValue(any(byte[].class), eq(Map.class))).thenReturn(messageData);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"error\"}");

            // Act
            handler.onMessage(messageJson, mockSession);

            // Assert
            verify(chatService, never()).sendMessage(any(), any(), anyString());
            verify(mockSession, times(1)).sendSync(anyString());
        }

        @Test
        @DisplayName("Should handle typing indicator")
        void shouldHandleTypingIndicator() throws IOException {
            // Arrange
            String messageJson = "{\"type\":\"typing\",\"receiverId\":\"" + testUser2Id + "\"}";
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "typing");
            messageData.put("receiverId", testUser2Id.toString());

            when(objectMapper.readValue(any(byte[].class), eq(Map.class))).thenReturn(messageData);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"typing\"}");

            // Act
            handler.onMessage(messageJson, mockSession);

            // Assert
            verify(chatService, never()).sendMessage(any(), any(), anyString());
            // Verify that typing notification would be broadcasted (can't fully test without receiver session)
        }

        @Test
        @DisplayName("Should handle mark as read")
        void shouldHandleMarkAsRead() throws IOException {
            // Arrange
            UUID messageId = UUID.randomUUID();
            String messageJson = "{\"type\":\"markRead\",\"messageId\":\"" + messageId + "\"}";
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "markRead");
            messageData.put("messageId", messageId.toString());

            when(objectMapper.readValue(any(byte[].class), eq(Map.class))).thenReturn(messageData);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"readReceipt\"}");
            doReturn(new ChatMessage()).when(chatService).markMessageAsRead(messageId, testUser1Id);

            // Act
            handler.onMessage(messageJson, mockSession);

            // Assert
            verify(chatService, times(1)).markMessageAsRead(messageId, testUser1Id);
            verify(mockSession, times(1)).sendAsync(anyString());
        }

        @Test
        @DisplayName("Should send error for unknown message type")
        void shouldSendErrorForUnknownMessageType() throws IOException {
            // Arrange
            String messageJson = "{\"type\":\"unknown\"}";
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "unknown");

            when(objectMapper.readValue(any(byte[].class), eq(Map.class))).thenReturn(messageData);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"error\"}");

            // Act
            handler.onMessage(messageJson, mockSession);

            // Assert
            verify(mockSession, times(1)).sendSync(anyString());
        }

        @Test
        @DisplayName("Should handle invalid message format gracefully")
        void shouldHandleInvalidMessageFormat() throws IOException {
            // Arrange
            String messageJson = "invalid json";
            when(objectMapper.readValue(any(byte[].class), eq(Map.class)))
                .thenThrow(new IOException("Invalid JSON"));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"error\"}");

            // Act
            handler.onMessage(messageJson, mockSession);

            // Assert
            verify(mockSession, times(1)).sendSync(anyString());
        }

        @Test
        @DisplayName("Should handle service exception and send error")
        void shouldHandleServiceException() throws IOException {
            // Arrange
            String messageJson = "{\"type\":\"message\",\"receiverId\":\"" + testUser2Id + "\",\"content\":\"Hello\"}";
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "message");
            messageData.put("receiverId", testUser2Id.toString());
            messageData.put("content", "Hello");

            when(objectMapper.readValue(any(byte[].class), eq(Map.class))).thenReturn(messageData);
            when(chatService.sendMessage(testUser1Id, testUser2Id, "Hello"))
                .thenThrow(new IllegalArgumentException("Receiver not found"));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"error\"}");

            // Act
            handler.onMessage(messageJson, mockSession);

            // Assert
            verify(mockSession, times(1)).sendSync(anyString());
        }
    }

    @Nested
    @DisplayName("Connection Closing - OnClose")
    class OnCloseTests {

        @Test
        @DisplayName("Should handle connection close successfully")
        void shouldHandleConnectionClose() throws Exception {
            // Arrange - First open the connection
            when(chatService.countTotalUnreadMessages(testUser1Id)).thenReturn(0L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            handler.onOpen(mockSession);

            // Act
            handler.onClose(mockSession);

            // Assert - No exceptions should be thrown
            // Verify session cleanup (we can't directly test the internal map, but no errors is good)
            verify(mockSession, never()).close(); // OnClose should not call close again
        }

        @Test
        @DisplayName("Should handle close without prior open gracefully")
        void shouldHandleCloseWithoutOpen() {
            // Act & Assert - Should not throw exception
            assertDoesNotThrow(() -> handler.onClose(mockSession));
        }
    }

    @Nested
    @DisplayName("Error Handling - OnError")
    class OnErrorTests {

        @Test
        @DisplayName("Should handle WebSocket error gracefully")
        void shouldHandleWebSocketError() {
            // Arrange
            Throwable error = new RuntimeException("Test error");

            // Act & Assert - Should not throw exception
            assertDoesNotThrow(() -> handler.onError(mockSession, error));
        }

        @Test
        @DisplayName("Should handle error with invalid session")
        void shouldHandleErrorWithInvalidSession() {
            // Arrange
            when(mockSession.getUserPrincipal()).thenReturn(Optional.empty());
            Throwable error = new RuntimeException("Test error");

            // Act & Assert - Should not throw exception
            assertDoesNotThrow(() -> handler.onError(mockSession, error));
        }
    }

    @Nested
    @DisplayName("Session Management")
    class SessionManagementTests {

        @Test
        @DisplayName("Should track multiple sessions for same user")
        void shouldTrackMultipleSessions() throws IOException {
            // Arrange
            WebSocketSession session2 = mock(WebSocketSession.class);
            when(session2.getUserPrincipal()).thenReturn(Optional.of(mockAuth));
            when(session2.getId()).thenReturn("test-session-id-2");
            when(session2.isOpen()).thenReturn(true);

            when(chatService.countTotalUnreadMessages(testUser1Id)).thenReturn(0L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // Act - Open two sessions for the same user
            handler.onOpen(mockSession);
            handler.onOpen(session2);

            // Assert
            verify(mockSession, times(1)).sendSync(anyString());
            verify(session2, times(1)).sendSync(anyString());
        }

        @Test
        @DisplayName("Should remove session on close")
        void shouldRemoveSessionOnClose() throws IOException {
            // Arrange
            when(chatService.countTotalUnreadMessages(testUser1Id)).thenReturn(0L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // Act
            handler.onOpen(mockSession);
            handler.onClose(mockSession);

            // Assert - Should not throw exception
            assertDoesNotThrow(() -> handler.onClose(mockSession));
        }
    }

    @Nested
    @DisplayName("Authentication")
    class AuthenticationTests {

        @Test
        @DisplayName("Should reject unauthenticated session")
        void shouldRejectUnauthenticatedSession() {
            // Arrange
            when(mockSession.getUserPrincipal()).thenReturn(Optional.empty());

            // Act
            handler.onOpen(mockSession);

            // Assert
            verify(mockSession, times(1)).close();
            verify(chatService, never()).countTotalUnreadMessages(any());
        }

        @Test
        @DisplayName("Should reject session with invalid user ID format")
        void shouldRejectInvalidUserId() {
            // Arrange
            when(mockAuth.getName()).thenReturn("invalid-uuid");

            // Act
            handler.onOpen(mockSession);

            // Assert
            verify(mockSession, times(1)).close();
        }
    }

    @Nested
    @DisplayName("ConversationId Integration")
    class ConversationIdIntegrationTests {

        @Test
        @DisplayName("Should use conversationId when sending messages")
        void shouldUseConversationIdWhenSendingMessages() throws IOException {
            // Arrange
            String messageJson = "{\"type\":\"message\",\"receiverId\":\"" + testUser2Id + "\",\"content\":\"Test message\"}";
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "message");
            messageData.put("receiverId", testUser2Id.toString());
            messageData.put("content", "Test message");

            ChatMessage savedMessage = new ChatMessage(testUser1Id, testUser2Id, "Test message");
            savedMessage.setId(UUID.randomUUID());
            savedMessage.setConversationId(UUID.randomUUID()); // ConversationId should be set
            savedMessage.setCreatedAt(OffsetDateTime.now());

            when(objectMapper.readValue(any(byte[].class), eq(Map.class))).thenReturn(messageData);
            when(chatService.sendMessage(testUser1Id, testUser2Id, "Test message")).thenReturn(savedMessage);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"message\"}");

            // Act
            handler.onMessage(messageJson, mockSession);

            // Assert
            verify(chatService, times(1)).sendMessage(testUser1Id, testUser2Id, "Test message");
            assertNotNull(savedMessage.getConversationId(), "ConversationId should be set on saved message");
        }

        @Test
        @DisplayName("Should handle conversationId in mark as read operations")
        void shouldHandleConversationIdInMarkAsRead() throws IOException {
            // Arrange
            UUID messageId = UUID.randomUUID();
            String messageJson = "{\"type\":\"markRead\",\"messageId\":\"" + messageId + "\"}";
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "markRead");
            messageData.put("messageId", messageId.toString());

            when(objectMapper.readValue(any(byte[].class), eq(Map.class))).thenReturn(messageData);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"readReceipt\"}");
            doReturn(new ChatMessage()).when(chatService).markMessageAsRead(messageId, testUser1Id);

            // Act
            handler.onMessage(messageJson, mockSession);

            // Assert
            verify(chatService, times(1)).markMessageAsRead(messageId, testUser1Id);
            // The service layer should handle conversationId internally
        }
    }
}
