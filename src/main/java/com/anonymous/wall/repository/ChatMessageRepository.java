package com.anonymous.wall.repository;

import com.anonymous.wall.entity.ChatMessage;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface ChatMessageRepository extends CrudRepository<ChatMessage, UUID> {

    /**
     * Find messages by room ID, ordered by created time (newest first)
     */
    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(UUID roomId);

    /**
     * Find recent messages in a room with limit
     */
    @Query("SELECT * FROM chat_messages WHERE room_id = :roomId AND is_deleted = false ORDER BY created_at DESC LIMIT :limit")
    List<ChatMessage> findRecentMessages(UUID roomId, int limit);

    /**
     * Find messages in a room with pagination
     */
    @Query("SELECT * FROM chat_messages WHERE room_id = :roomId AND is_deleted = false ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<ChatMessage> findMessagesWithPagination(UUID roomId, int limit, int offset);

    /**
     * Count messages in a room
     */
    long countByRoomId(UUID roomId);

    /**
     * Count undeleted messages in a room
     */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE room_id = :roomId AND is_deleted = false")
    long countActiveMessages(UUID roomId);
}
