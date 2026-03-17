package com.anonymous.wall.repository;

import com.anonymous.wall.entity.Conversation;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.ORACLE)
public interface ConversationRepository extends CrudRepository<Conversation, UUID> {
    List<Conversation> findByUserIdOrderByLastMessageAtDesc(UUID userId);
    Optional<Conversation> findByConversationIdAndUserId(UUID conversationId, UUID userId);
    void deleteByConversationId(UUID conversationId);
}