package com.anonymous.wall.repository;

import com.anonymous.wall.entity.Post;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface PostRepository extends CrudRepository<Post, UUID> {

    // ===== Sorting by Created Time (Default) =====
    /**
     * Find posts by wall, sorted by created time (newest first)
     */
    Page<Post> findByWallOrderByCreatedAtDesc(String wall, Pageable pageable);

    /**
     * Find posts by wall, sorted by created time (oldest first)
     */
    Page<Post> findByWallOrderByCreatedAtAsc(String wall, Pageable pageable);

    /**
     * Find campus posts by wall and domain, sorted by created time (newest first)
     */
    Page<Post> findByWallAndSchoolDomainOrderByCreatedAtDesc(String wall, String schoolDomain, Pageable pageable);

    /**
     * Find campus posts by wall and domain, sorted by created time (oldest first)
     */
    Page<Post> findByWallAndSchoolDomainOrderByCreatedAtAsc(String wall, String schoolDomain, Pageable pageable);

    // ===== Sorting by Like Count =====
    /**
     * Find posts by wall, sorted by like count (most liked first)
     */
    Page<Post> findByWallOrderByLikeCountDesc(String wall, Pageable pageable);

    /**
     * Find posts by wall, sorted by like count (least liked first)
     */
    Page<Post> findByWallOrderByLikeCountAsc(String wall, Pageable pageable);

    /**
     * Find campus posts by wall and domain, sorted by like count (most liked first)
     */
    Page<Post> findByWallAndSchoolDomainOrderByLikeCountDesc(String wall, String schoolDomain, Pageable pageable);

    /**
     * Find campus posts by wall and domain, sorted by like count (least liked first)
     */
    Page<Post> findByWallAndSchoolDomainOrderByLikeCountAsc(String wall, String schoolDomain, Pageable pageable);

    // ===== Other methods =====
    /**
     * Count posts by wall type
     */
    long countByWall(String wall);

    /**
     * Count campus posts by school domain
     */
    long countByWallAndSchoolDomain(String wall, String schoolDomain);

    /**
     * Find all posts by user ID
     */
    List<Post> findByUserId(UUID userId);

    /**
     * Find post by ID
     */
    Optional<Post> findById(UUID id);

    // ===== Filter Hidden Posts =====
    /**
     * Find non-hidden posts by wall, sorted by created time (newest first)
     */
    Page<Post> findByWallAndHiddenFalseOrderByCreatedAtDesc(String wall, Pageable pageable);

    /**
     * Find non-hidden posts by wall, sorted by created time (oldest first)
     */
    Page<Post> findByWallAndHiddenFalseOrderByCreatedAtAsc(String wall, Pageable pageable);

    /**
     * Find non-hidden campus posts by wall and domain, sorted by created time (newest first)
     */
    Page<Post> findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc(String wall, String schoolDomain, Pageable pageable);

    /**
     * Find non-hidden campus posts by wall and domain, sorted by created time (oldest first)
     */
    Page<Post> findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtAsc(String wall, String schoolDomain, Pageable pageable);

    /**
     * Find non-hidden posts by wall, sorted by like count (most liked first)
     */
    Page<Post> findByWallAndHiddenFalseOrderByLikeCountDesc(String wall, Pageable pageable);

    /**
     * Find non-hidden posts by wall, sorted by like count (least liked first)
     */
    Page<Post> findByWallAndHiddenFalseOrderByLikeCountAsc(String wall, Pageable pageable);

    /**
     * Find non-hidden campus posts by wall and domain, sorted by like count (most liked first)
     */
    Page<Post> findByWallAndSchoolDomainAndHiddenFalseOrderByLikeCountDesc(String wall, String schoolDomain, Pageable pageable);

    /**
     * Find non-hidden campus posts by wall and domain, sorted by like count (least liked first)
     */
    Page<Post> findByWallAndSchoolDomainAndHiddenFalseOrderByLikeCountAsc(String wall, String schoolDomain, Pageable pageable);

    // ===== User's Own Posts =====
    /**
     * Find non-hidden posts by a user with pagination, sorted by created time (newest first)
     */
    Page<Post> findByUserIdAndHiddenFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find non-hidden posts by a user with pagination, sorted by created time (oldest first)
     */
    Page<Post> findByUserIdAndHiddenFalseOrderByCreatedAtAsc(UUID userId, Pageable pageable);

    /**
     * Find non-hidden posts by a user with pagination, sorted by like count (most liked first)
     */
    Page<Post> findByUserIdAndHiddenFalseOrderByLikeCountDesc(UUID userId, Pageable pageable);

    /**
     * Find non-hidden posts by a user with pagination, sorted by like count (least liked first)
     */
    Page<Post> findByUserIdAndHiddenFalseOrderByLikeCountAsc(UUID userId, Pageable pageable);

    /**
     * Count non-hidden posts by a user
     */
    long countByUserIdAndHiddenFalse(UUID userId);

    /**
     * Update profile name for all posts by a user
     * Used for profile name propagation when user changes their profile name
     * Micronaut Data automatically generates: UPDATE posts SET profile_name = ? WHERE user_id = ?
     */
    void updateProfileNameByUserId(UUID userId, String profileName);
    
    /**
     * Find all posts with pagination (for admin purposes)
     */
    Page<Post> findAll(Pageable pageable);

    /**
     * Find all posts by hidden status with pagination (for admin purposes)
     */
    Page<Post> findByHidden(boolean hidden, Pageable pageable);

    /**
     * Find all posts by user ID with pagination (for admin purposes)
     */
    Page<Post> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find by hidden status with pagination (for admin purposes)
     */
    Page<Post> findByUserIdAndHidden(UUID userId, boolean hidden, Pageable pageable);
    
    // ===== Admin sorting - by creation time =====
    
    /**
     * Find all posts sorted by creation time (newest first) - for admin
     */
    Page<Post> findAllOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Find all posts sorted by creation time (oldest first) - for admin
     */
    Page<Post> findAllOrderByCreatedAtAsc(Pageable pageable);
    
    // ===== Admin sorting - by like count =====
    
    /**
     * Find all posts sorted by like count (most likes first) - for admin
     */
    Page<Post> findAllOrderByLikeCountDesc(Pageable pageable);
    
    /**
     * Find all posts sorted by like count (least likes first) - for admin
     */
    Page<Post> findAllOrderByLikeCountAsc(Pageable pageable);
    
    // ===== Admin sorting - by comment count =====
    
    /**
     * Find all posts sorted by comment count (most comments first) - for admin
     */
    Page<Post> findAllOrderByCommentCountDesc(Pageable pageable);
    
    /**
     * Find all posts sorted by comment count (least comments first) - for admin
     */
    Page<Post> findAllOrderByCommentCountAsc(Pageable pageable);
    
    // ===== Admin sorting - by author =====
    
    /**
     * Find all posts sorted by user ID (author) ascending - for admin
     */
    Page<Post> findAllOrderByUserIdAsc(Pageable pageable);
    
    /**
     * Find all posts sorted by user ID (author) descending - for admin
     */
    Page<Post> findAllOrderByUserIdDesc(Pageable pageable);
    
    // ===== Admin filtering by wall =====
    
    /**
     * Find posts by wall type with pagination - for admin
     */
    Page<Post> findByWall(String wall, Pageable pageable);
    
    /**
     * Find posts by wall type and hidden status with pagination - for admin
     */
    Page<Post> findByWallAndHidden(String wall, boolean hidden, Pageable pageable);
    
    /**
     * Find posts by wall type and user ID with pagination - for admin
     */
    Page<Post> findByWallAndUserId(String wall, UUID userId, Pageable pageable);
    
    /**
     * Find posts by wall type, user ID, and hidden status with pagination - for admin
     */
    Page<Post> findByWallAndUserIdAndHidden(String wall, UUID userId, boolean hidden, Pageable pageable);
}
