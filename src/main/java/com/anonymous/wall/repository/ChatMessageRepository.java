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
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface ChatMessageRepository extends CrudRepository<ChatMessage, UUID> {

    /**
     * Find all messages between two users (conversation history).
     * Results are ordered by created_at ascending (oldest first).
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    @Query(value = "SELECT * FROM chat_messages WHERE " +
           "(sender_id = :userId1 AND receiver_id = :userId2) OR " +
           "(sender_id = :userId2 AND receiver_id = :userId1) " +
           "ORDER BY created_at ASC",
           countQuery = "SELECT COUNT(*) FROM chat_messages WHERE " +
           "(sender_id = :userId1 AND receiver_id = :userId2) OR " +
           "(sender_id = :userId2 AND receiver_id = :userId1)")
    Page<ChatMessage> findConversationBetweenUsers(UUID userId1, UUID userId2, Pageable pageable);

    /**
     * Find all messages between two users (conversation history) as a list.
     * Results are ordered by created_at ascending (oldest first).
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return List of messages
     */
    @Query("SELECT * FROM chat_messages WHERE " +
           "(sender_id = :userId1 AND receiver_id = :userId2) OR " +
           "(sender_id = :userId2 AND receiver_id = :userId1) " +
           "ORDER BY created_at ASC")
    List<ChatMessage> findConversationBetweenUsers(UUID userId1, UUID userId2);

    /**
     * Count unread messages for a receiver from a specific sender.
     * 
     * @param receiverId The receiver's user ID
     * @param senderId The sender's user ID
     * @return Count of unread messages
     */
    long countByReceiverIdAndSenderIdAndReadStatusFalse(UUID receiverId, UUID senderId);

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
     * Mark all messages from a sender to a receiver as read.
     * 
     * @param receiverId The receiver's user ID
     * @param senderId The sender's user ID
     */
    @Query("UPDATE chat_messages SET read_status = true WHERE receiver_id = :receiverId AND sender_id = :senderId AND read_status = false")
    void markMessagesAsRead(UUID receiverId, UUID senderId);

    /**
     * Get list of users with whom the given user has conversations.
     * Returns distinct user IDs who have either sent messages to or received messages from the given user.
     * 
     * @param userId The user ID
     * @return List of user IDs
     */
    @Query("SELECT DISTINCT CASE " +
           "WHEN sender_id = :userId THEN receiver_id " +
           "ELSE sender_id END as user_id " +
           "FROM chat_messages " +
           "WHERE sender_id = :userId OR receiver_id = :userId")
    List<UUID> findConversationPartners(UUID userId);

    /**
     * Get the last message in a conversation between two users.
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return The last message or null
     */
    @Query("SELECT * FROM chat_messages WHERE " +
           "(sender_id = :userId1 AND receiver_id = :userId2) OR " +
           "(sender_id = :userId2 AND receiver_id = :userId1) " +
           "ORDER BY created_at DESC LIMIT 1")
    ChatMessage findLastMessageBetweenUsers(UUID userId1, UUID userId2);
}
