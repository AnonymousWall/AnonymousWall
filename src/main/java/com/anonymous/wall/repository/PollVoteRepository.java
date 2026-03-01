package com.anonymous.wall.repository;

import com.anonymous.wall.entity.PollVote;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.ORACLE)
public interface PollVoteRepository extends CrudRepository<PollVote, UUID> {

    /**
     * Find a user's vote for a specific poll post
     */
    Optional<PollVote> findByPostIdAndUserId(UUID postId, UUID userId);

    /**
     * Delete all votes for a post
     */
    long deleteByPostId(UUID postId);
}
