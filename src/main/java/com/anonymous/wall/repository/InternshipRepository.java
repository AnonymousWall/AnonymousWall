package com.anonymous.wall.repository;

import com.anonymous.wall.entity.Internship;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface InternshipRepository extends CrudRepository<Internship, UUID> {

    /**
     * Find all non-hidden internships with pagination, sorted by created time (newest first)
     */
    Page<Internship> findByHiddenOrderByCreatedAtDesc(boolean hidden, Pageable pageable);

    /**
     * Find all non-hidden internships with pagination, sorted by created time (oldest first)
     */
    Page<Internship> findByHiddenOrderByCreatedAtAsc(boolean hidden, Pageable pageable);

    /**
     * Find internships by user ID
     */
    List<Internship> findByUserId(UUID userId);

    /**
     * Find internships by user ID with pagination
     */
    Page<Internship> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find internship by ID
     */
    Optional<Internship> findById(UUID id);

    /**
     * Count internships by user ID
     */
    long countByUserId(UUID userId);
}
