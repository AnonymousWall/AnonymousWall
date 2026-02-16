package com.anonymous.wall.repository;

import com.anonymous.wall.entity.ChatMessage;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
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
     * Count unread messages in a conversation for a specific receiver.
     * 
     * @param conversationId The conversation ID
     * @param receiverId The receiver's user ID
     * @return Count of unread messages
     */
    long countByConversationIdAndReceiverIdAndReadStatusFalse(UUID conversationId, UUID receiverId);

    /**
     * Find unread messages in a conversation for a specific receiver.
     *
     * @param conversationId The conversation ID
     * @param receiverId The receiver's user ID
     * @return List of unread messages
     */
    List<ChatMessage> findByConversationIdAndReceiverIdAndReadStatusFalse(UUID conversationId, UUID receiverId);

    /**
     * Count all unread messages for a receiver.
     * 
     * @param receiverId The receiver's user ID
     * @return Count of all unread messages
     */
    long countByReceiverIdAndReadStatusFalse(UUID receiverId);

    void updateReadStatusByConversationIdAndReceiverId(UUID conversationId, UUID receiverId, boolean readStatus);

    List<UUID> findDistinctConversationIdBySenderIdOrReceiverId(UUID senderId, UUID receiverId);

    Optional<ChatMessage> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}
