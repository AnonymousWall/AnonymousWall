package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.CommentReport;
import com.anonymous.wall.entity.PostReport;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

/**
 * Service interface for admin report management operations
 */
public interface AdminReportService {
    
    Page<PostReport> getAllPostReports(Pageable pageable);
    
    Page<CommentReport> getAllCommentReports(Pageable pageable);

    PostReport getPostReportById(UUID id);

    CommentReport getCommentReportById(UUID id);

    void resolvePostReport(UUID id);

    void resolveCommentReport(UUID id);

    void rejectPostReport(UUID id);

    void rejectCommentReport(UUID id);
}
