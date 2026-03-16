package com.anonymous.wall.repository;

import com.anonymous.wall.entity.PostLike;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.ORACLE)
public interface PostLikeRepository extends CrudRepository<PostLike, UUID> {

    /**
     * Check if a user has liked a post
     */
    Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId);

    /**
     * Batch query: Find all likes by a user for multiple posts
     * This method is used to avoid N+1 query problem when enriching multiple posts
     */
    List<PostLike> findByUserIdAndPostIdIn(UUID userId, List<UUID> postIds);

    /**
     * Count likes for a post
     */
    long countByPostId(UUID postId);

    /**
     * Delete a like by post and user
     */
    long deleteByPostIdAndUserId(UUID postId, UUID userId);

    /**
     * Delete all likes for a post (useful for post deletion)
     */
    long deleteByPostId(UUID postId);

    List<PostLike> findByUserId(UUID userId);
}
