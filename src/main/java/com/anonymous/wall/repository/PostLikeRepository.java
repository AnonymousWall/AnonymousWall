package com.anonymous.wall.repository;

import com.anonymous.wall.entity.PostLike;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface PostLikeRepository extends CrudRepository<PostLike, Long> {

    /**
     * Check if a user has liked a post
     */
    Optional<PostLike> findByPostIdAndUserId(Long postId, UUID userId);

    /**
     * Count likes for a post
     */
    long countByPostId(Long postId);

    /**
     * Delete a like by post and user
     */
    long deleteByPostIdAndUserId(Long postId, UUID userId);

    /**
     * Delete all likes for a post (useful for post deletion)
     */
    long deleteByPostId(Long postId);
}
