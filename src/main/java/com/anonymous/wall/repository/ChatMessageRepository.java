package com.anonymous.wall.repository;

import com.anonymous.wall.entity.ChatMessage;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing chat messages in the database.
 * Provides methods for querying messages, conversations, and unread counts.
 * Uses conversationId for efficient querying and indexing.
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface ChatMessageRepository extends CrudRepository<ChatMessage, UUID> {

    /**
     * Find all messages in a conversation by conversation ID.
     * Results are ordered by created_at ascending (oldest first).
     * 
     * @param conversationId The conversation ID
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    Page<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    /**
     * Find all messages in a conversation by conversation ID as a list.
     * Results are ordered by created_at ascending (oldest first).
     * 
     * @param conversationId The conversation ID
     * @return List of messages
     */
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /**
     * Count unread messages in a conversation for a specific receiver.
     * 
     * @param conversationId The conversation ID
     * @param receiverId The receiver's user ID
     * @return Count of unread messages
     */
    long countByConversationIdAndReceiverIdAndReadStatusFalse(UUID conversationId, UUID receiverId);

    /**
     * Count all unread messages for a receiver.
     * 
     * @param receiverId The receiver's user ID
     * @return Count of all unread messages
     */
    long countByReceiverIdAndReadStatusFalse(UUID receiverId);

    /**
     * Find all messages sent to a receiver.
     * 
     * @param receiverId The receiver's user ID
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    Page<ChatMessage> findByReceiverIdOrderByCreatedAtDesc(UUID receiverId, Pageable pageable);

    /**
     * Find all messages sent by a sender.
     * 
     * @param senderId The sender's user ID
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    Page<ChatMessage> findBySenderIdOrderByCreatedAtDesc(UUID senderId, Pageable pageable);

    /**
     * Mark all messages in a conversation as read for a specific receiver.
     * 
     * @param conversationId The conversation ID
     * @param receiverId The receiver's user ID
     */
    @Query("UPDATE chat_messages SET read_status = true WHERE conversation_id = :conversationId AND receiver_id = :receiverId AND read_status = false")
    void markConversationMessagesAsRead(UUID conversationId, UUID receiverId);

    /**
     * Get list of unique conversation IDs for a user.
     * Returns distinct conversation IDs where the user is either sender or receiver.
     * 
     * @param userId The user ID
     * @return List of conversation IDs
     */
    @Query("SELECT DISTINCT conversation_id FROM chat_messages WHERE sender_id = :userId OR receiver_id = :userId")
    List<UUID> findUserConversations(UUID userId);

    /**
     * Get the last message in a conversation.
     * 
     * @param conversationId The conversation ID
     * @return The last message or null
     */
    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY created_at DESC LIMIT 1")
    ChatMessage findLastMessageInConversation(UUID conversationId);

    /**
     * Get the other participant's user ID in a conversation.
     * Given a conversation ID and one user's ID, returns the other user's ID.
     * 
     * @param conversationId The conversation ID
     * @param userId The known user's ID
     * @return The other user's ID
     */
    @Query("SELECT CASE WHEN sender_id = :userId THEN receiver_id ELSE sender_id END as other_user_id " +
           "FROM chat_messages WHERE conversation_id = :conversationId LIMIT 1")
    UUID findOtherParticipantInConversation(UUID conversationId, UUID userId);
}
