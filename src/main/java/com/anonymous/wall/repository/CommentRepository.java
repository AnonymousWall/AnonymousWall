package com.anonymous.wall.repository;

import com.anonymous.wall.entity.Comment;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.ORACLE)
public interface CommentRepository extends CrudRepository<Comment, UUID> {

    // ===== Parent-based queries (replacing postId-based queries) =====

    List<Comment> findByParentTypeAndParentId(String parentType, UUID parentId);

    List<Comment> findByParentTypeAndParentIdAndHiddenFalse(String parentType, UUID parentId);

    // ===== Sorting by Created Time (Default) =====

    Page<Comment> findByParentTypeAndParentIdOrderByCreatedAtDesc(String parentType, UUID parentId, Pageable pageable);

    Page<Comment> findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtDesc(String parentType, UUID parentId, Pageable pageable);

    Page<Comment> findByParentTypeAndParentIdOrderByCreatedAtAsc(String parentType, UUID parentId, Pageable pageable);

    Page<Comment> findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtAsc(String parentType, UUID parentId, Pageable pageable);

    Page<Comment> findByParentTypeAndParentId(String parentType, UUID parentId, Pageable pageable);

    Page<Comment> findByParentTypeAndParentIdAndHiddenFalse(String parentType, UUID parentId, Pageable pageable);

    long countByParentTypeAndParentId(String parentType, UUID parentId);

    long countByParentTypeAndParentIdAndHiddenFalse(String parentType, UUID parentId);

    long deleteByParentTypeAndParentId(String parentType, UUID parentId);

    Comment update(Comment comment);

    List<Comment> findAllByParentTypeAndParentId(String parentType, UUID parentId);

    void updateByParentTypeAndParentId(String parentType, UUID parentId, boolean hidden);

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

    // ===== Admin parentId/parentType filters =====
    Page<Comment> findByParentId(UUID parentId, Pageable pageable);
    Page<Comment> findByParentIdAndHidden(UUID parentId, boolean hidden, Pageable pageable);
    Page<Comment> findByParentIdAndUserId(UUID parentId, UUID userId, Pageable pageable);
    Page<Comment> findByParentIdAndUserIdAndHidden(UUID parentId, UUID userId, boolean hidden, Pageable pageable);

    Page<Comment> findByParentTypeOrderByCreatedAtDesc(String parentType, Pageable pageable);
    Page<Comment> findByParentTypeOrderByCreatedAtAsc(String parentType, Pageable pageable);
    Page<Comment> findByParentTypeAndHidden(String parentType, boolean hidden, Pageable pageable);
    Page<Comment> findByParentTypeAndUserId(String parentType, UUID userId, Pageable pageable);
    Page<Comment> findByParentTypeAndUserIdAndHidden(String parentType, UUID userId, boolean hidden, Pageable pageable);
    Page<Comment> findByParentTypeAndParentIdAndHidden(String parentType, UUID parentId, boolean hidden, Pageable pageable);
    
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
