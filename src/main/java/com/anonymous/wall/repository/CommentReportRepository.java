package com.anonymous.wall.repository;

import com.anonymous.wall.entity.CommentReport;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
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
}
