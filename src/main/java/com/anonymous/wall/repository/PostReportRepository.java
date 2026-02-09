package com.anonymous.wall.repository;

import com.anonymous.wall.entity.PostReport;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface PostReportRepository extends CrudRepository<PostReport, UUID> {
    
    /**
     * Check if a user has already reported a post
     */
    boolean existsByPostIdAndReporterUserId(UUID postId, UUID reporterUserId);
    
    /**
     * Find a report by post and reporter
     */
    Optional<PostReport> findByPostIdAndReporterUserId(UUID postId, UUID reporterUserId);
    
    /**
     * Count reports for a specific post
     */
    long countByPostId(UUID postId);
}
