package com.anonymous.wall.controller;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.model.ChatMessageDTO;
import com.anonymous.wall.service.ChatService;
import com.anonymous.wall.service.RedisPubSubService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.*;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time chat messaging.
 * Manages WebSocket connections, message broadcasting, and user presence.
 * 
 * WebSocket endpoint: ws://host/ws/chat
 * Requires authentication via JWT token in query parameter.
 */
@ServerWebSocket("/ws/chat")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ChatWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    @Inject
    private ChatService chatService;

    @Inject
    private WebSocketBroadcaster broadcaster;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private RedisPubSubService redisPubSubService;

    // Map to track user sessions: userId -> Set of sessions
    private final Map<UUID, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    /**
     * Handle new WebSocket connection.
     * Only authenticated users can connect.
     */
    @OnOpen
    public void onOpen(WebSocketSession session) {
        try {
            UUID userId = getUserIdFromSession(session);
            
            // Add session to user's session set; track whether this is the first session atomically
            boolean[] isFirstSession = {false};
            userSessions.compute(userId, (k, sessions) -> {
                if (sessions == null) {
                    sessions = ConcurrentHashMap.newKeySet();
                    isFirstSession[0] = true;
                }
                sessions.add(session);
                return sessions;
            });

            // Only subscribe once per user per instance (on first session)
            if (isFirstSession[0]) {
                redisPubSubService.subscribe(userId, message -> deliverToLocalSessions(userId, message));
            }
            
            log.info("WebSocket connection opened for user: {}, session: {}", userId, session.getId());
            
            // Send connection confirmation
            Map<String, Object> response = new HashMap<>();
            response.put("type", "connected");
            response.put("userId", userId.toString());
            response.put("timestamp", System.currentTimeMillis());
            
            session.sendSync(serializeToJson(response));
            
            // Notify about unread messages count
            long unreadCount = chatService.countTotalUnreadMessages(userId);
            if (unreadCount > 0) {
                Map<String, Object> unreadNotification = new HashMap<>();
                unreadNotification.put("type", "unreadCount");
                unreadNotification.put("count", unreadCount);
                session.sendSync(serializeToJson(unreadNotification));
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket connection", e);
            session.close();
        }
    }

    /**
     * Handle incoming WebSocket messages.
     * Expected message format:
     * {
     *   "type": "message",
     *   "receiverId": "uuid",
     *   "content": "message text"
     * }
     */
    @OnMessage
    public void onMessage(String message, WebSocketSession session) {
        try {
            UUID senderId = getUserIdFromSession(session);
            log.debug("Received WebSocket message from user: {}", senderId);

            // Parse message
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(message.getBytes(), Map.class);
            String type = (String) messageData.get("type");
            log.debug("Received WebSocket message type: {}", type);
            if ("message".equals(type)) {
                // Handle chat message
                String receiverIdStr = (String) messageData.get("receiverId");
                String content = (String) messageData.get("content");
                String imageUrl = (String) messageData.get("imageUrl");

                if (receiverIdStr == null) {
                    sendError(session, "Missing receiverId");
                    return;
                }

                boolean hasContent = content != null && !content.trim().isEmpty();
                boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();
                if (!hasContent && !hasImage) {
                    sendError(session, "Message must have content or an image");
                    return;
                }

                UUID receiverId = UUID.fromString(receiverIdStr);

                // Save message via service
                ChatMessage chatMessage = chatService.sendMessage(senderId, receiverId, content, imageUrl);

                // Convert to DTO
                ChatMessageDTO messageDTO = convertToDTO(chatMessage);

                // Build response
                Map<String, Object> response = new HashMap<>();
                response.put("type", "message");
                response.put("message", messageDTO);

                String responseJson = serializeToJson(response);

                // Send to sender (confirmation)
                session.sendAsync(responseJson);

                // Send to receiver if online
                broadcastToUser(receiverId, responseJson);

                log.info("WebSocket message delivered from {} to {}", senderId, receiverId);

            } else if ("typing".equals(type)) {
                // Handle typing indicator
                String receiverIdStr = (String) messageData.get("receiverId");
                if (receiverIdStr != null) {
                    UUID receiverId = UUID.fromString(receiverIdStr);
                    Map<String, Object> typingNotification = new HashMap<>();
                    typingNotification.put("type", "typing");
                    typingNotification.put("senderId", senderId.toString());
                    broadcastToUser(receiverId, serializeToJson(typingNotification));
                }

            } else if ("markRead".equals(type)) {
                log.debug("Received WebSocket markRead message from user: {}", senderId);
                handleMarkAsRead(messageData, senderId, session);
            } else {
                sendError(session, "Unknown message type: " + type);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid WebSocket message: {}", e.getMessage());
            sendError(session, e.getMessage());
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            sendError(session, "Internal server error");
        }
    }

    private void handleMarkAsRead(
            Map<String, Object> messageData,
            UUID readerId,
            WebSocketSession session) {

        String messageIdStr = (String) messageData.get("messageId");
        if (messageIdStr != null) {
            try {
                UUID messageId = UUID.fromString(messageIdStr);

                // mark as read and get message
                ChatMessage message = chatService.markMessageAsRead(messageId, readerId);

                // 1. send read receipt to reader (confirmation)
                Map<String, Object> readerReceipt = new HashMap<>();
                readerReceipt.put("type", "readReceipt");
                readerReceipt.put("messageId", messageIdStr);
                session.sendAsync(serializeToJson(readerReceipt));

                // 2. ✅ notify original sender (critical fix)
                UUID originalSenderId = message.getSenderId();
                if (!originalSenderId.equals(readerId)) {  // not self-sent message
                    Map<String, Object> senderNotification = new HashMap<>();
                    senderNotification.put("type", "markRead");
                    senderNotification.put("messageId", messageIdStr);
                    senderNotification.put("readBy", readerId.toString());
                    senderNotification.put("readAt", System.currentTimeMillis());

                    broadcastToUser(originalSenderId, serializeToJson(senderNotification));

                    log.debug("Notified sender {} that message {} was read by {}",
                            originalSenderId, messageId, readerId);
                    log.debug("Sending markRead notification for messageId: {} to sender: {}",
                            messageIdStr, originalSenderId);
                }

                // 3. ✅ NEW: Send updated unread count to reader
                long unreadCount = chatService.countTotalUnreadMessages(readerId);
                Map<String, Object> unreadUpdate = new HashMap<>();
                unreadUpdate.put("type", "unreadCount");
                unreadUpdate.put("count", unreadCount);
                broadcastToUser(readerId, serializeToJson(unreadUpdate));

                log.debug("Updated unread count for reader {}: {}", readerId, unreadCount);


            } catch (IllegalArgumentException e) {
                sendError(session, "Invalid messageId or permission denied");
            }
        }
    }

    /**
     * Handle WebSocket connection close.
     */
    @OnClose
    public void onClose(WebSocketSession session) {
        try {
            UUID userId = getUserIdFromSession(session);
            
            // Remove session from user's session set
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    redisPubSubService.unsubscribe(userId);
                }
            }
            
            log.info("WebSocket connection closed for user: {}, session: {}", userId, session.getId());
        } catch (Exception e) {
            log.warn("Error handling WebSocket close", e);
        }
    }

    /**
     * Handle WebSocket errors.
     */
    @OnError
    public void onError(WebSocketSession session, Throwable throwable) {
        try {
            UUID userId = getUserIdFromSession(session);
            log.error("WebSocket error for user: {}, session: {}", userId, session.getId(), throwable);
        } catch (Exception e) {
            log.error("WebSocket error", throwable);
        }
    }

    /**
     * Extract user ID from authenticated WebSocket session.
     */
    private UUID getUserIdFromSession(WebSocketSession session) {
        Optional<?> userPrincipal = session.getUserPrincipal();
        
        if (userPrincipal.isEmpty()) {
            throw new IllegalStateException("WebSocket session not authenticated");
        }

        Object principal = userPrincipal.get();
        
        if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;
            String name = auth.getName();
            try {
                return UUID.fromString(name);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid user ID in authentication: " + name, e);
            }
        }
        
        throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
    }

    /**
     * Broadcast message to a specific user via Redis Pub/Sub.
     * Redis routes the message to whichever instance has the user's WebSocket session.
     */
    public void broadcastToUser(UUID userId, String message) {
        redisPubSubService.publish(userId, message);
    }

    /**
     * Deliver a message to all local WebSocket sessions for a user.
     * Called by the Redis subscriber callback.
     */
    private void deliverToLocalSessions(UUID userId, String message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null && !sessions.isEmpty()) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendAsync(message);
                }
            }
            log.debug("Delivered Redis message to user {} ({} local sessions)", userId, sessions.size());
        }
        // If no local sessions: user is on another instance, that instance's subscriber will deliver it.
    }

    /**
     * Send error message to a session.
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("type", "error");
            error.put("error", errorMessage);
            session.sendSync(serializeToJson(error));
        } catch (Exception e) {
            log.error("Failed to send error message", e);
        }
    }

    /**
     * Helper method to serialize object to JSON string.
     */
    public String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (IOException e) {
            log.error("Failed to serialize object to JSON", e);
            throw new RuntimeException("JSON serialization error", e);
        }
    }

    /**
     * Helper method to convert ChatMessage entity to DTO.
     */
    private ChatMessageDTO convertToDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setContent(message.getContent());
        dto.setImageUrl(message.getImageUrl());
        dto.setReadStatus(message.isReadStatus());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}
