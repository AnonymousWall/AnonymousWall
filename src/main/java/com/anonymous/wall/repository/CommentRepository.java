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
     * Find non-hidden comments by a user with pagination, sorted by created time (newest first)
     */
    Page<Comment> findByUserIdAndHiddenFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find non-hidden comments by a user with pagination, sorted by created time (oldest first)
     */
    Page<Comment> findByUserIdAndHiddenFalseOrderByCreatedAtAsc(UUID userId, Pageable pageable);

    /**
     * Count non-hidden comments by a user
     */
    long countByUserIdAndHiddenFalse(UUID userId);

    /**
     * Update profile name for all comments by a user
     * Used for profile name propagation when user changes their profile name
     * Micronaut Data automatically generates: UPDATE comments SET profile_name = ? WHERE user_id = ?
     */
    void updateProfileNameByUserId(UUID userId, String profileName);
    
    /**
     * Find all comments with pagination (for admin purposes)
     */
    Page<Comment> findAll(Pageable pageable);

    /**
     * Find all comments by hidden status with pagination (for admin purposes)
     */
    Page<Comment> findByHidden(boolean hidden, Pageable pageable);

    /**
     * Find all comments for a user with pagination (for admin purposes)
     */
    Page<Comment> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find all comments for a user with pagination and hidden status (for admin purposes)
     */
    Page<Comment> findByUserIdAndHidden(UUID userId, boolean hidden, Pageable pageable);
    
    // ===== Admin sorting - by creation time =====
    
    /**
     * Find all comments sorted by creation time (newest first) - for admin
     */
    Page<Comment> findAllOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Find all comments sorted by creation time (oldest first) - for admin
     */
    Page<Comment> findAllOrderByCreatedAtAsc(Pageable pageable);
    
    // ===== Admin sorting - by author =====
    
    /**
     * Find all comments sorted by user ID (author) ascending - for admin
     */
    Page<Comment> findAllOrderByUserIdAsc(Pageable pageable);
    
    /**
     * Find all comments sorted by user ID (author) descending - for admin
     */
    Page<Comment> findAllOrderByUserIdDesc(Pageable pageable);

}
