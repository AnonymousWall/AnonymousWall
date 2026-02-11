package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.CommentReport;
import com.anonymous.wall.entity.PostReport;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

/**
 * Service interface for admin report management operations
 */
public interface AdminReportService {
    
    /**
     * Get all post reports with pagination
     */
    Page<PostReport> getAllPostReports(Pageable pageable);
    
    /**
     * Get all comment reports with pagination
     */
    Page<CommentReport> getAllCommentReports(Pageable pageable);
}
