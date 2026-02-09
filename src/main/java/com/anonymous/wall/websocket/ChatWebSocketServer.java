package com.anonymous.wall.websocket;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.service.ChatService;
import com.anonymous.wall.service.JwtTokenService;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.*;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket server for real-time chat functionality
 * 
 * Endpoint: ws://host/ws/chat/{roomId}?token={JWT}
 * 
 * This server handles:
 * - JWT authentication via query parameter
 * - Room membership verification
 * - Real-time message broadcasting
 * - Message persistence
 */
@ServerWebSocket("/ws/chat/{roomId}")
public class ChatWebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketServer.class);

    @Inject
    private ChatService chatService;

    @Inject
    private JwtTokenService jwtTokenService;

    // Map of room ID -> Set of WebSocket sessions
    private final Map<UUID, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // Map of session -> user ID
    private final Map<WebSocketSession, UUID> sessionUsers = new ConcurrentHashMap<>();

    /**
     * Called when a WebSocket connection is opened
     */
    @OnOpen
    public void onOpen(String roomId, WebSocketSession session) {
        log.info("WebSocket connection attempt: roomId={}, sessionId={}", roomId, session.getId());

        try {
            UUID roomUUID = UUID.fromString(roomId);

            // 1. Extract and validate JWT token from query parameter
            String token = session.getRequestParameter("token").orElse(null);
            if (token == null || token.trim().isEmpty()) {
                log.warn("No token provided, closing connection");
                session.close(CloseReason.POLICY_VIOLATION.getCode(), "Authentication required");
                return;
            }

            // 2. Validate token and get user ID
            UUID userId;
            try {
                userId = jwtTokenService.extractUserIdFromToken(token);
                if (userId == null) {
                    log.warn("Invalid token, closing connection");
                    session.close(CloseReason.POLICY_VIOLATION.getCode(), "Invalid token");
                    return;
                }
            } catch (Exception e) {
                log.error("Error validating token: {}", e.getMessage());
                session.close(CloseReason.POLICY_VIOLATION.getCode(), "Invalid token");
                return;
            }

            // 3. Verify user has access to room (is a member)
            if (!chatService.canAccessRoom(userId, roomUUID)) {
                log.warn("User not authorized for room: userId={}, roomId={}", userId, roomId);
                session.close(CloseReason.POLICY_VIOLATION.getCode(), "Not authorized");
                return;
            }

            // 4. Add session to room
            roomSessions.computeIfAbsent(roomUUID, k -> ConcurrentHashMap.newKeySet()).add(session);
            sessionUsers.put(session, userId);

            log.info("WebSocket connection established: userId={}, roomId={}, sessionId={}", 
                userId, roomId, session.getId());

            // 5. Send recent message history to newly connected client
            try {
                List<ChatMessage> history = chatService.getRecentMessages(roomUUID, 50);
                session.sendSync(Map.of(
                    "type", "history",
                    "messages", history
                ));
                log.debug("Sent message history: count={}", history.size());
            } catch (Exception e) {
                log.error("Error sending message history: {}", e.getMessage());
            }

            // 6. Update last read timestamp
            chatService.updateLastRead(roomUUID, userId);

        } catch (IllegalArgumentException e) {
            log.error("Invalid room ID: {}", roomId);
            session.close(CloseReason.PROTOCOL_ERROR.getCode(), "Invalid room ID");
        } catch (Exception e) {
            log.error("Error handling WebSocket connection: {}", e.getMessage(), e);
            session.close(CloseReason.UNEXPECTED_CONDITION.getCode(), "Server error");
        }
    }

    /**
     * Called when a message is received from a client
     */
    @OnMessage
    public void onMessage(String roomId, String message, WebSocketSession session) {
        log.debug("WebSocket message received: roomId={}, sessionId={}", roomId, session.getId());

        try {
            UUID roomUUID = UUID.fromString(roomId);
            UUID userId = sessionUsers.get(session);

            if (userId == null) {
                log.warn("User ID not found for session, closing");
                session.close(CloseReason.POLICY_VIOLATION.getCode(), "Session not authenticated");
                return;
            }

            // Validate message content
            if (message == null || message.trim().isEmpty()) {
                log.warn("Empty message received from userId={}", userId);
                sendError(session, "Message cannot be empty");
                return;
            }

            if (message.length() > 5000) {
                log.warn("Message too long from userId={}: length={}", userId, message.length());
                sendError(session, "Message too long (max 5000 characters)");
                return;
            }

            // Save message to database
            ChatMessage chatMessage = chatService.sendMessage(roomUUID, userId, message.trim());

            // Broadcast message to all sessions in the room
            Set<WebSocketSession> sessions = roomSessions.get(roomUUID);
            if (sessions != null) {
                Map<String, Object> messageData = Map.of(
                    "type", "message",
                    "message", chatMessage
                );

                for (WebSocketSession s : sessions) {
                    try {
                        s.sendAsync(messageData);
                    } catch (Exception e) {
                        log.error("Error broadcasting to session: {}", e.getMessage());
                    }
                }
            }

            log.debug("Message broadcasted: messageId={}, roomId={}", chatMessage.getId(), roomId);

        } catch (IllegalArgumentException e) {
            log.error("Invalid room ID in message: {}", roomId);
            sendError(session, "Invalid room ID");
        } catch (SecurityException e) {
            log.error("Security error: {}", e.getMessage());
            sendError(session, "Not authorized");
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage(), e);
            sendError(session, "Error processing message");
        }
    }

    /**
     * Called when a WebSocket connection is closed
     */
    @OnClose
    public void onClose(String roomId, WebSocketSession session) {
        log.info("WebSocket connection closed: roomId={}, sessionId={}", roomId, session.getId());

        try {
            UUID roomUUID = UUID.fromString(roomId);
            UUID userId = sessionUsers.get(session);

            // Remove session from room
            Set<WebSocketSession> sessions = roomSessions.get(roomUUID);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessions.remove(roomUUID);
                    log.debug("Room is now empty, removed from map: roomId={}", roomId);
                }
            }

            // Remove user mapping
            sessionUsers.remove(session);

            // Update last read timestamp
            if (userId != null) {
                chatService.updateLastRead(roomUUID, userId);
            }

            log.debug("Session cleaned up successfully");

        } catch (Exception e) {
            log.error("Error during WebSocket close: {}", e.getMessage());
        }
    }

    /**
     * Called when an error occurs on the WebSocket connection
     */
    @OnError
    public void onError(String roomId, WebSocketSession session, Throwable error) {
        log.error("WebSocket error: roomId={}, sessionId={}, error={}", 
            roomId, session.getId(), error.getMessage(), error);

        // Clean up the session
        onClose(roomId, session);
    }

    /**
     * Send an error message to a specific session
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            session.sendSync(Map.of(
                "type", "error",
                "error", errorMessage
            ));
        } catch (Exception e) {
            log.error("Error sending error message: {}", e.getMessage());
        }
    }
}
