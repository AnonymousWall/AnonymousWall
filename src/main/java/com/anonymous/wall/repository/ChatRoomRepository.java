package com.anonymous.wall.repository;

import com.anonymous.wall.entity.ChatRoom;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface ChatRoomRepository extends CrudRepository<ChatRoom, UUID> {

    /**
     * Find chat rooms by type
     */
    List<ChatRoom> findByType(String type);

    /**
     * Find campus chat rooms by type and school domain
     */
    List<ChatRoom> findByTypeAndSchoolDomain(String type, String schoolDomain);

    /**
     * Find rooms created by a specific user
     */
    List<ChatRoom> findByCreatedBy(UUID createdBy);
}
