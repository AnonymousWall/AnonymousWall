package com.anonymous.wall.repository;

import com.anonymous.wall.entity.PollOption;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.ORACLE)
public interface PollOptionRepository extends CrudRepository<PollOption, UUID> {

    /**
     * Get all options for a poll post ordered by display order
     */
    List<PollOption> findByPostIdOrderByDisplayOrder(UUID postId);

    /**
     * Delete all options for a post
     */
    long deleteByPostId(UUID postId);
}
