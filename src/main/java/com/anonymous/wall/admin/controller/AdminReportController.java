package com.anonymous.wall.admin.controller;

import com.anonymous.wall.admin.service.AdminReportService;
import com.anonymous.wall.entity.CommentReport;
import com.anonymous.wall.entity.PostReport;
import com.anonymous.wall.model.AdminCommentReportDTO;
import com.anonymous.wall.model.AdminPostReportDTO;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin controller for report management
 */
@Controller("/api/v1/admin/reports")
public class AdminReportController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminReportController.class);
    
    @Inject
    private AdminReportService adminReportService;
    
    /**
     * Convert PostReport entity to AdminPostReportDTO
     */
    private AdminPostReportDTO mapPostReportToDTO(PostReport report) {
        AdminPostReportDTO dto = new AdminPostReportDTO();
        dto.setId(report.getId());
        dto.setPostId(report.getPostId());
        dto.setReporterUserId(report.getReporterUserId());
        dto.setReportedUserId(report.getReportedUserId());
        dto.setReason(report.getReason());
        dto.setCreatedAt(report.getCreatedAt());
        return dto;
    }
    
    /**
     * Convert CommentReport entity to AdminCommentReportDTO
     */
    private AdminCommentReportDTO mapCommentReportToDTO(CommentReport report) {
        AdminCommentReportDTO dto = new AdminCommentReportDTO();
        dto.setId(report.getId());
        dto.setCommentId(report.getCommentId());
        dto.setReporterUserId(report.getReporterUserId());
        dto.setReportedUserId(report.getReportedUserId());
        dto.setReason(report.getReason());
        dto.setCreatedAt(report.getCreatedAt());
        return dto;
    }
    
    /**
     * GET /admin/reports - List all reports with pagination
     */
    @Get
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getAllReports(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String type,
            HttpRequest<?> request) {
        
        log.info("Admin fetching reports - page: {}, limit: {}, type: {}", page, limit, type);
        
        // Validate pagination parameters
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        // Create Pageable (0-based indexing)
        Pageable pageable = Pageable.from(page - 1, limit);
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        
        // Track pagination info from the first query
        Page<?> paginationSource = null;
        
        // Fetch reports based on type filter
        if (type == null || "post".equals(type)) {
            Page<PostReport> postReportsPage = adminReportService.getAllPostReports(pageable);
            List<AdminPostReportDTO> postReportDTOs = postReportsPage.getContent().stream()
                    .map(this::mapPostReportToDTO)
                    .collect(Collectors.toList());
            response.put("postReports", postReportDTOs);
            paginationSource = postReportsPage;
        }
        
        if (type == null || "comment".equals(type)) {
            Page<CommentReport> commentReportsPage = adminReportService.getAllCommentReports(pageable);
            List<AdminCommentReportDTO> commentReportDTOs = commentReportsPage.getContent().stream()
                    .map(this::mapCommentReportToDTO)
                    .collect(Collectors.toList());
            response.put("commentReports", commentReportDTOs);
            if (paginationSource == null) {
                paginationSource = commentReportsPage;
            }
        }
        
        // Add pagination info from first query
        if (paginationSource != null) {
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("limit", limit);
            pagination.put("total", paginationSource.getTotalSize());
            pagination.put("totalPages", paginationSource.getTotalPages());
            response.put("pagination", pagination);
        }
        
        return HttpResponse.ok(response);
    }
}
