package com.anonymous.wall.repository;

import com.anonymous.wall.entity.NotificationEntity;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface NotificationRepository extends CrudRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndRead(UUID recipientUserId, boolean read);

    void updateReadByRecipientUserId(UUID recipientUserId, boolean read);

    void updateReadById(UUID id, boolean read);

    Optional<NotificationEntity> findById(UUID id);
}
