package com.anonymous.wall.controller;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.entity.ChatRoom;
import com.anonymous.wall.entity.RoomMember;
import com.anonymous.wall.service.ChatService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST API controller for chat functionality
 * Provides endpoints for:
 * - Creating and managing chat rooms
 * - Retrieving message history
 * - Managing room membership
 */
@Controller("/api/v1/chat")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Inject
    private ChatService chatService;

    /**
     * Create a new direct chat room with another user
     * POST /api/v1/chat/rooms
     */
    @Post("/rooms")
    public HttpResponse<?> createRoom(@Body CreateRoomRequest request, Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());

            // Validate other user ID
            if (request.getOtherUserId() == null || request.getOtherUserId().trim().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "Other user ID is required"));
            }

            UUID otherUserId;
            try {
                otherUserId = UUID.fromString(request.getOtherUserId());
            } catch (IllegalArgumentException e) {
                return HttpResponse.badRequest(Map.of("error", "Invalid user ID format"));
            }

            // Cannot create room with yourself
            if (userId.equals(otherUserId)) {
                return HttpResponse.badRequest(Map.of("error", "Cannot create a chat room with yourself"));
            }

            // Create direct room
            ChatRoom room = chatService.createDirectRoom(userId, otherUserId);

            log.info("Direct chat room created: id={}, user1={}, user2={}", room.getId(), userId, otherUserId);
            return HttpResponse.created(room);

        } catch (Exception e) {
            log.error("Error creating chat room: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to create room"));
        }
    }

    /**
     * Get user's chat rooms
     * GET /api/v1/chat/rooms
     */
    @Get("/rooms")
    public HttpResponse<?> getUserRooms(Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            List<ChatRoom> rooms = chatService.getUserRooms(userId);
            return HttpResponse.ok(rooms);
        } catch (Exception e) {
            log.error("Error retrieving user rooms: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to retrieve rooms"));
        }
    }

    /**
     * Get a specific room by ID
     * GET /api/v1/chat/rooms/{roomId}
     */
    @Get("/rooms/{roomId}")
    public HttpResponse<?> getRoom(@PathVariable String roomId, Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID roomUUID = UUID.fromString(roomId);

            // Verify user has access
            if (!chatService.canAccessRoom(userId, roomUUID)) {
                return HttpResponse.unauthorized();
            }

            Optional<ChatRoom> roomOpt = chatService.getRoom(roomUUID);
            if (roomOpt.isEmpty()) {
                return HttpResponse.notFound();
            }

            return HttpResponse.ok(roomOpt.get());
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", "Invalid room ID"));
        } catch (Exception e) {
            log.error("Error retrieving room: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to retrieve room"));
        }
    }

    /**
     * Get messages for a room
     * GET /api/v1/chat/rooms/{roomId}/messages
     */
    @Get("/rooms/{roomId}/messages")
    public HttpResponse<?> getRoomMessages(
        @PathVariable String roomId,
        @QueryValue(defaultValue = "50") int limit,
        @QueryValue(defaultValue = "0") int offset,
        Authentication authentication
    ) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID roomUUID = UUID.fromString(roomId);

            // Verify user has access
            if (!chatService.canAccessRoom(userId, roomUUID)) {
                return HttpResponse.unauthorized();
            }

            List<ChatMessage> messages;
            if (offset > 0) {
                messages = chatService.getMessages(roomUUID, limit, offset);
            } else {
                messages = chatService.getRecentMessages(roomUUID, limit);
            }

            return HttpResponse.ok(messages);
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", "Invalid room ID"));
        } catch (Exception e) {
            log.error("Error retrieving messages: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to retrieve messages"));
        }
    }

    /**
     * Add a member to a room
     * POST /api/v1/chat/rooms/{roomId}/members
     */
    @Post("/rooms/{roomId}/members")
    public HttpResponse<?> addMember(
        @PathVariable String roomId,
        @Body AddMemberRequest request,
        Authentication authentication
    ) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID roomUUID = UUID.fromString(roomId);
            UUID newMemberUUID = UUID.fromString(request.getUserId());

            // Verify user has access to the room
            if (!chatService.canAccessRoom(userId, roomUUID)) {
                return HttpResponse.unauthorized();
            }

            // Add the new member (service layer enforces 2-member limit)
            chatService.addMember(roomUUID, newMemberUUID);

            log.info("Member added to room: roomId={}, newMember={}, addedBy={}", roomId, request.getUserId(), userId);
            return HttpResponse.ok(Map.of("message", "Member added successfully"));

        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", "Invalid ID format"));
        } catch (IllegalStateException e) {
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding member: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to add member"));
        }
    }

    /**
     * Get members of a room
     * GET /api/v1/chat/rooms/{roomId}/members
     */
    @Get("/rooms/{roomId}/members")
    public HttpResponse<?> getRoomMembers(
        @PathVariable String roomId,
        Authentication authentication
    ) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID roomUUID = UUID.fromString(roomId);

            // Verify user has access
            if (!chatService.canAccessRoom(userId, roomUUID)) {
                return HttpResponse.unauthorized();
            }

            List<RoomMember> members = chatService.getRoomMembers(roomUUID);
            return HttpResponse.ok(members);

        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", "Invalid room ID"));
        } catch (Exception e) {
            log.error("Error retrieving members: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to retrieve members"));
        }
    }

    /**
     * Delete a message (soft delete)
     * DELETE /api/v1/chat/messages/{messageId}
     */
    @Delete("/messages/{messageId}")
    public HttpResponse<?> deleteMessage(
        @PathVariable String messageId,
        Authentication authentication
    ) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID messageUUID = UUID.fromString(messageId);

            chatService.deleteMessage(messageUUID, userId);

            log.info("Message deleted: messageId={}, userId={}", messageId, userId);
            return HttpResponse.ok(Map.of("message", "Message deleted successfully"));

        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", "Invalid message ID"));
        } catch (SecurityException e) {
            return HttpResponse.unauthorized();
        } catch (Exception e) {
            log.error("Error deleting message: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("error", "Failed to delete message"));
        }
    }

    // Request DTOs
    public static class CreateRoomRequest {
        private String otherUserId;

        public String getOtherUserId() { return otherUserId; }
        public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }
    }

    public static class AddMemberRequest {
        private String userId;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
