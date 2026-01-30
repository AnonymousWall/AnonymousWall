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
public interface PostRepository extends CrudRepository<Post, Long> {

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
    Optional<Post> findById(Long id);

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
}
