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

    /**
     * Find posts by wall type with pagination
     */
    Page<Post> findByWall(String wall, Pageable pageable);

    /**
     * Find national posts with pagination
     */
    Page<Post> findByWallOrderByCreatedAtDesc(String wall, Pageable pageable);

    /**
     * Find campus posts by school domain with pagination
     */
    Page<Post> findByWallAndSchoolDomain(String wall, String schoolDomain, Pageable pageable);

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
    List<Post> findByUserId(java.util.UUID userId);

    /**
     * Find post by ID and eager load counts
     */
    Optional<Post> findById(Long id);
}
