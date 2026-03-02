package com.anonymous.wall.repository;

import com.anonymous.wall.entity.NotificationEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.UUID;

@JdbcRepository(dialect = Dialect.ORACLE)
public interface NotificationRepository extends CrudRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndRead(UUID recipientUserId, boolean read);

    @Query("UPDATE notifications SET read = TRUE WHERE recipient_user_id = :recipientUserId")
    void markAllReadByRecipientUserId(UUID recipientUserId);

    @Query("UPDATE notifications SET read = TRUE WHERE id = :id")
    void markReadById(UUID id);
}
