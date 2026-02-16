package com.anonymous.wall.service;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.model.ConversationDTO;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing chat messages.
 * Handles message sending, retrieval, and validation including blocked user checks.
 */
public interface ChatService {

    /**
     * Send a message from one user to another.
     * Validates that the receiver is not blocked and both users exist.
     * 
     * @param senderId ID of the sender
     * @param receiverId ID of the receiver
     * @param content Message content
     * @return The saved ChatMessage
     * @throws IllegalArgumentException if receiver is blocked or doesn't exist
     */
    ChatMessage sendMessage(UUID senderId, UUID receiverId, String content);

    /**
     * Get message history between two users with pagination.
     * Messages are returned in chronological order (oldest first).
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    Page<ChatMessage> getMessageHistory(UUID userId1, UUID userId2, Pageable pageable);

    /**
     * Get list of conversations for a user.
     * Each conversation includes the other user's info, last message, and unread count.
     * 
     * @param userId The user's ID
     * @return List of conversation DTOs
     */
    List<ConversationDTO> getConversations(UUID userId);

    /**
     * Mark a specific message as read.
     * 
     * @param messageId The message ID
     * @param userId The user marking the message as read (must be the receiver)
     * @throws IllegalArgumentException if message not found or user is not the receiver
     */
    ChatMessage markMessageAsRead(UUID messageId, UUID userId);

    /**
     * Mark all messages from a sender to a receiver as read.
     * 
     * @param receiverId The receiver's ID
     * @param senderId The sender's ID
     */
    void markConversationAsRead(UUID receiverId, UUID senderId);

    /**
     * Count unread messages for a user from a specific sender.
     * 
     * @param receiverId The receiver's ID
     * @param senderId The sender's ID
     * @return Count of unread messages
     */
    long countUnreadMessages(UUID receiverId, UUID senderId);

    /**
     * Count total unread messages for a user.
     * 
     * @param receiverId The receiver's ID
     * @return Total count of unread messages
     */
    long countTotalUnreadMessages(UUID receiverId);

    List<ChatMessage> getUnreadMessages(UUID receiverId, UUID senderId);
}
