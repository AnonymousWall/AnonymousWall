package com.anonymous.wall.repository;

import com.anonymous.wall.entity.UserEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface UserRepository extends CrudRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    
    /**
     * Find all users with pagination
     */
    Page<UserEntity> findAll(Pageable pageable);
    
    // ===== Sorting by basic fields =====
    
    /**
     * Find all users sorted by creation time (newest first)
     */
    Page<UserEntity> findAllOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Find all users sorted by creation time (oldest first)
     */
    Page<UserEntity> findAllOrderByCreatedAtAsc(Pageable pageable);
    
    /**
     * Find all users sorted by school domain
     */
    Page<UserEntity> findAllOrderBySchoolDomain(Pageable pageable);
    
    /**
     * Find all users sorted by report count (most reports first)
     */
    Page<UserEntity> findAllOrderByReportCountDesc(Pageable pageable);
    
    /**
     * Find all users sorted by report count (least reports first)
     */
    Page<UserEntity> findAllOrderByReportCountAsc(Pageable pageable);
    
    // ===== Filtering by blocked status =====
    
    /**
     * Find blocked users with pagination
     */
    Page<UserEntity> findByBlocked(boolean blocked, Pageable pageable);
    
    /**
     * Find blocked users sorted by creation time (newest first)
     */
    Page<UserEntity> findByBlockedOrderByCreatedAtDesc(boolean blocked, Pageable pageable);
    
    /**
     * Find blocked users sorted by creation time (oldest first)
     */
    Page<UserEntity> findByBlockedOrderByCreatedAtAsc(boolean blocked, Pageable pageable);
    
    // ===== Sorting by post count (requires JOIN with posts table) =====
    
    /**
     * Find all users with post count, sorted by post count (most posts first)
     */
    @Query(value = "SELECT u.* FROM users u " +
           "LEFT JOIN (SELECT user_id, COUNT(*) as post_count FROM posts WHERE is_hidden = false GROUP BY user_id) p ON u.id = p.user_id " +
           "ORDER BY COALESCE(p.post_count, 0) DESC",
           countQuery = "SELECT COUNT(*) FROM users")
    Page<UserEntity> findAllOrderByPostCountDesc(Pageable pageable);
    
    /**
     * Find all users with post count, sorted by post count (least posts first)
     */
    @Query(value = "SELECT u.* FROM users u " +
           "LEFT JOIN (SELECT user_id, COUNT(*) as post_count FROM posts WHERE is_hidden = false GROUP BY user_id) p ON u.id = p.user_id " +
           "ORDER BY COALESCE(p.post_count, 0) ASC",
           countQuery = "SELECT COUNT(*) FROM users")
    Page<UserEntity> findAllOrderByPostCountAsc(Pageable pageable);
    
    // ===== Sorting by comment count (requires JOIN with comments table) =====
    
    /**
     * Find all users with comment count, sorted by comment count (most comments first)
     */
    @Query(value = "SELECT u.* FROM users u " +
           "LEFT JOIN (SELECT user_id, COUNT(*) as comment_count FROM comments WHERE is_hidden = false GROUP BY user_id) c ON u.id = c.user_id " +
           "ORDER BY COALESCE(c.comment_count, 0) DESC",
           countQuery = "SELECT COUNT(*) FROM users")
    Page<UserEntity> findAllOrderByCommentCountDesc(Pageable pageable);
    
    /**
     * Find all users with comment count, sorted by comment count (least comments first)
     */
    @Query(value = "SELECT u.* FROM users u " +
           "LEFT JOIN (SELECT user_id, COUNT(*) as comment_count FROM comments WHERE is_hidden = false GROUP BY user_id) c ON u.id = c.user_id " +
           "ORDER BY COALESCE(c.comment_count, 0) ASC",
           countQuery = "SELECT COUNT(*) FROM users")
    Page<UserEntity> findAllOrderByCommentCountAsc(Pageable pageable);
}
