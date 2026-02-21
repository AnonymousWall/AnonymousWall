package com.anonymous.wall.repository;

import com.anonymous.wall.entity.CommentReport;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface CommentReportRepository extends CrudRepository<CommentReport, UUID> {
    
    /**
     * Check if a user has already reported a comment
     */
    boolean existsByCommentIdAndReporterUserId(UUID commentId, UUID reporterUserId);
    
    /**
     * Find a report by comment and reporter
     */
    Optional<CommentReport> findByCommentIdAndReporterUserId(UUID commentId, UUID reporterUserId);
    
    /**
     * Count reports for a specific comment
     */
    long countByCommentId(UUID commentId);
    
    /**
     * Find all comment reports with pagination
     */
    Page<CommentReport> findAll(Pageable pageable);

    Page<CommentReport> findAllOrderByCreatedAtDesc(Pageable pageable);

    Page<CommentReport> findByReporterUserId(UUID reporterUserId, Pageable pageable);

    Page<CommentReport> findByReportedUserId(UUID reportedUserId, Pageable pageable);

    Page<CommentReport> findByStatus(String status, Pageable pageable);

    CommentReport update(CommentReport report);
}
