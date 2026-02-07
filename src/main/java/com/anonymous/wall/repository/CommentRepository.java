package com.anonymous.wall.repository;

import com.anonymous.wall.entity.Comment;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface CommentRepository extends CrudRepository<Comment, UUID> {

    /**
     * Find all comments for a post
     */
    List<Comment> findByPostId(UUID postId);

    /**
     * Find all non-hidden comments for a post
     */
    List<Comment> findByPostIdAndHiddenFalse(UUID postId);

    // ===== Sorting by Created Time (Default) =====
    /**
     * Find comments for a post with pagination, sorted by created time (newest first)
     */
    Page<Comment> findByPostIdOrderByCreatedAtDesc(UUID postId, Pageable pageable);

    /**
     * Find non-hidden comments for a post with pagination, sorted by created time (newest first)
     */
    Page<Comment> findByPostIdAndHiddenFalseOrderByCreatedAtDesc(UUID postId, Pageable pageable);

    /**
     * Find comments for a post with pagination, sorted by created time (oldest first)
     */
    Page<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId, Pageable pageable);

    /**
     * Find non-hidden comments for a post with pagination, sorted by created time (oldest first)
     */
    Page<Comment> findByPostIdAndHiddenFalseOrderByCreatedAtAsc(UUID postId, Pageable pageable);

    /**
     * Find comments for a post with pagination (generic - for compatibility)
     */
    Page<Comment> findByPostId(UUID postId, Pageable pageable);

    /**
     * Find non-hidden comments for a post with pagination (generic)
     */
    Page<Comment> findByPostIdAndHiddenFalse(UUID postId, Pageable pageable);

    /**
     * Count comments for a post
     */
    long countByPostId(UUID postId);

    /**
     * Count non-hidden comments for a post
     */
    long countByPostIdAndHiddenFalse(UUID postId);

    /**
     * Delete all comments for a post (useful for post deletion)
     */
    long deleteByPostId(UUID postId);

    /**
     * Update a comment (used for hiding/unhiding)
     */
    Comment update(Comment comment);

    /**
     * Find all comments for a post (including hidden ones)
     */
    List<Comment> findAllByPostId(UUID postId);

    void updateByPostId(UUID postId, boolean hidden);
//    void updateHiddenTrueByPostId(Long postId);

    // ===== User's Own Comments =====
    /**
     * Find all comments by a user with pagination, sorted by created time (newest first)
     */
    Page<Comment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find all comments by a user with pagination, sorted by created time (oldest first)
     */
    Page<Comment> findByUserIdOrderByCreatedAtAsc(UUID userId, Pageable pageable);

    /**
     * Find non-hidden comments by a user with pagination, sorted by created time (newest first)
     */
    Page<Comment> findByUserIdAndHiddenFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find non-hidden comments by a user with pagination, sorted by created time (oldest first)
     */
    Page<Comment> findByUserIdAndHiddenFalseOrderByCreatedAtAsc(UUID userId, Pageable pageable);

    /**
     * Count all comments by a user
     */
    long countByUserId(UUID userId);

    /**
     * Count non-hidden comments by a user
     */
    long countByUserIdAndHiddenFalse(UUID userId);

}
