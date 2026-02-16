package com.anonymous.wall.controller;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.model.ChatMessageDTO;
import com.anonymous.wall.model.ConversationDTO;
import com.anonymous.wall.model.SendMessageRequest;
import com.anonymous.wall.service.ChatService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for chat functionality.
 * Provides endpoints for sending messages, retrieving history, and managing conversations.
 */
@Controller("/api/v1/chat")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Inject
    private ChatService chatService;

    @Inject
    private ChatWebSocketHandler chatWebSocketHandler;

    /**
     * Helper to extract user ID from Principal
     */
    private UUID getUserIdFromRequest(HttpRequest<?> request) {
        Optional<Principal> principalOpt = request.getUserPrincipal();

        if (principalOpt.isEmpty()) {
            throw new IllegalArgumentException("User not authenticated");
        }

        String principalName = principalOpt.get().getName();
        try {
            return UUID.fromString(principalName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user ID format in security context: " + principalName, e);
        }
    }

    /**
     * GET /chat/conversations
     * Get list of conversations for the authenticated user
     */
    @Get("/conversations")
    public HttpResponse<?> getConversations(HttpRequest<?> request) {
        try {
            UUID userId = getUserIdFromRequest(request);
            log.debug("Getting conversations for user {}", userId);

            List<ConversationDTO> conversations = chatService.getConversations(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("conversations", conversations);

            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error getting conversations: {}", e.getMessage());
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error getting conversations", e);
            return HttpResponse.serverError(Map.of("error", "Internal server error"));
        }
    }

    /**
     * GET /chat/messages/{otherUserId}
     * Get message history with another user
     */
    @Get("/messages/{otherUserId}")
    public HttpResponse<?> getMessageHistory(
            @PathVariable String otherUserId,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "50") int limit,
            HttpRequest<?> request) {
        try {
            UUID userId = getUserIdFromRequest(request);
            UUID otherUserUUID = UUID.fromString(otherUserId);

            log.debug("Getting message history between {} and {}", userId, otherUserUUID);

            // Create pageable
            Pageable pageable = Pageable.from(page - 1, limit);

            // Get messages
            Page<ChatMessage> messagesPage = chatService.getMessageHistory(userId, otherUserUUID, pageable);

            // Convert to DTOs
            List<ChatMessageDTO> messageDTOs = messagesPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("messages", messageDTOs);

            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("limit", limit);
            pagination.put("total", messagesPage.getTotalSize());
            pagination.put("totalPages", messagesPage.getTotalPages());
            response.put("pagination", pagination);

            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error getting message history: {}", e.getMessage());
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error getting message history", e);
            return HttpResponse.serverError(Map.of("error", "Internal server error"));
        }
    }

    /**
     * POST /chat/messages
     * Send a chat message
     */
    @Post("/messages")
    public HttpResponse<?> sendMessage(@Body SendMessageRequest request, HttpRequest<?> httpRequest) {
        try {
            UUID senderId = getUserIdFromRequest(httpRequest);
            UUID receiverId = request.getReceiverId();

            log.debug("Sending message from {} to {}", senderId, receiverId);

            // Send message
            ChatMessage message = chatService.sendMessage(senderId, receiverId, request.getContent());

            // Convert to DTO
            ChatMessageDTO messageDTO = convertToDTO(message);

            return HttpResponse.created(messageDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Error sending message: {}", e.getMessage());
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error sending message", e);
            return HttpResponse.serverError(Map.of("error", "Internal server error"));
        }
    }

    /**
     * PUT /chat/messages/{messageId}/read
     * Mark a specific message as read
     */
    @Put("/messages/{messageId}/read")
    public HttpResponse<?> markMessageAsRead(
            @PathVariable String messageId,
            HttpRequest<?> request) {
        try {
            UUID userId = getUserIdFromRequest(request);
            UUID messageUUID = UUID.fromString(messageId);

            log.debug("Marking message {} as read by user {}", messageUUID, userId);

            chatService.markMessageAsRead(messageUUID, userId);

            return HttpResponse.ok(Map.of("message", "Message marked as read"));
        } catch (IllegalArgumentException e) {
            log.warn("Error marking message as read: {}", e.getMessage());
            if (e.getMessage().contains("Only the receiver")) {
                return HttpResponse.<Map<String, String>>status(io.micronaut.http.HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            }
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error marking message as read", e);
            return HttpResponse.serverError(Map.of("error", "Internal server error"));
        }
    }

    /**
     * PUT /chat/conversations/{otherUserId}/read
     * Mark all messages from a user as read
     */
    @Put("/conversations/{otherUserId}/read")
    public HttpResponse<?> markConversationAsRead(
            @PathVariable String otherUserId,
            HttpRequest<?> request) {
        try {
            UUID userId = getUserIdFromRequest(request);
            UUID otherUserUUID = UUID.fromString(otherUserId);

            log.debug("Marking all messages from {} to {} as read", otherUserUUID, userId);

            List<ChatMessage> unreadMessages = chatService.getUnreadMessages(userId, otherUserUUID);

            chatService.markConversationAsRead(userId, otherUserUUID);

            for (ChatMessage message : unreadMessages) {
                UUID senderId = message.getSenderId();

                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "markRead");
                notification.put("messageId", message.getId().toString());
                notification.put("readBy", userId.toString());
                notification.put("readAt", System.currentTimeMillis());

                // Send notification to sender
                chatWebSocketHandler.broadcastToUser(senderId, chatWebSocketHandler.serializeToJson(notification));
                log.debug("Notified sender {} that message {} was read by {}", senderId, message.getId(), userId);
            }

            return HttpResponse.ok(Map.of("message", "Conversation marked as read"));
        } catch (IllegalArgumentException e) {
            log.warn("Error marking conversation as read: {}", e.getMessage());
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error marking conversation as read", e);
            return HttpResponse.serverError(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Helper method to convert ChatMessage entity to DTO
     */
    private ChatMessageDTO convertToDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setContent(message.getContent());
        dto.setReadStatus(message.isReadStatus());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}
