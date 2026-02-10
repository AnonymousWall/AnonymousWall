package com.anonymous.wall.repository;

import com.anonymous.wall.entity.ChatRoom;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface ChatRoomRepository extends CrudRepository<ChatRoom, UUID> {

    /**
     * Find rooms created by a specific user
     */
    List<ChatRoom> findByCreatedBy(UUID createdBy);
}
