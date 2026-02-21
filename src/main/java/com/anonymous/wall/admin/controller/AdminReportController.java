package com.anonymous.wall.admin.controller;

import com.anonymous.wall.admin.service.AdminReportService;
import com.anonymous.wall.entity.CommentReport;
import com.anonymous.wall.entity.PostReport;
import com.anonymous.wall.model.AdminCommentReportDTO;
import com.anonymous.wall.model.AdminPostReportDTO;
import com.anonymous.wall.model.AdminReportDTO;
import com.anonymous.wall.model.AdminGetReportByIdTypeParameter;
import com.anonymous.wall.model.AdminReportDTOStatus;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin controller for report management
 */
@Controller("/api/v1/admin/reports")
public class AdminReportController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminReportController.class);
    
    @Inject
    private AdminReportService adminReportService;

    private AdminReportDTO mapToAdminReportDTO(PostReport report) {
        AdminReportDTO dto = new AdminReportDTO();
        dto.setId(report.getId());
        dto.setType(AdminGetReportByIdTypeParameter.POST);
        dto.setTargetId(report.getPostId());
        dto.setReporterUserId(report.getReporterUserId());
        dto.setReportedUserId(report.getReportedUserId());
        dto.setReason(report.getReason());
        dto.setStatus(AdminReportDTOStatus.fromValue(report.getStatus()));
        dto.setCreatedAt(report.getCreatedAt());
        return dto;
    }

    private AdminReportDTO mapToAdminReportDTO(CommentReport report) {
        AdminReportDTO dto = new AdminReportDTO();
        dto.setId(report.getId());
        dto.setType(AdminGetReportByIdTypeParameter.COMMENT);
        dto.setTargetId(report.getCommentId());
        dto.setReporterUserId(report.getReporterUserId());
        dto.setReportedUserId(report.getReportedUserId());
        dto.setReason(report.getReason());
        dto.setStatus(AdminReportDTOStatus.fromValue(report.getStatus()));
        dto.setCreatedAt(report.getCreatedAt());
        return dto;
    }
    
    @Get
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getAllReports(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String type,
            HttpRequest<?> request) {
        
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        Pageable pageable = Pageable.from(page - 1, limit);
        Map<String, Object> response = new HashMap<>();
        Page<?> paginationSource = null;
        
        if (type == null || "post".equals(type)) {
            Page<PostReport> postReportsPage = adminReportService.getAllPostReports(pageable);
            List<AdminReportDTO> postReportDTOs = postReportsPage.getContent().stream()
                    .map(this::mapToAdminReportDTO)
                    .collect(Collectors.toList());
            response.put("postReports", postReportDTOs);
            paginationSource = postReportsPage;
        }
        
        if (type == null || "comment".equals(type)) {
            Page<CommentReport> commentReportsPage = adminReportService.getAllCommentReports(pageable);
            List<AdminReportDTO> commentReportDTOs = commentReportsPage.getContent().stream()
                    .map(this::mapToAdminReportDTO)
                    .collect(Collectors.toList());
            response.put("commentReports", commentReportDTOs);
            if (paginationSource == null) {
                paginationSource = commentReportsPage;
            }
        }
        
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

    @Get("/{id}")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<AdminReportDTO> getReportById(
            @PathVariable String id,
            @QueryValue String type) {
        log.info("Admin fetching report by id: {}, type: {}", id, type);
        UUID reportId = UUID.fromString(id);
        AdminReportDTO dto;
        if ("POST".equalsIgnoreCase(type)) {
            dto = mapToAdminReportDTO(adminReportService.getPostReportById(reportId));
        } else {
            dto = mapToAdminReportDTO(adminReportService.getCommentReportById(reportId));
        }
        return HttpResponse.ok(dto);
    }

    @Put("/{id}/resolve")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> resolveReport(
            @PathVariable String id,
            @QueryValue String type) {
        log.info("Admin resolving report: {}, type: {}", id, type);
        UUID reportId = UUID.fromString(id);
        if ("POST".equalsIgnoreCase(type)) {
            adminReportService.resolvePostReport(reportId);
        } else {
            adminReportService.resolveCommentReport(reportId);
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "Report resolved successfully");
        return HttpResponse.ok(response);
    }

    @Put("/{id}/reject")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> rejectReport(
            @PathVariable String id,
            @QueryValue String type) {
        log.info("Admin rejecting report: {}, type: {}", id, type);
        UUID reportId = UUID.fromString(id);
        if ("POST".equalsIgnoreCase(type)) {
            adminReportService.rejectPostReport(reportId);
        } else {
            adminReportService.rejectCommentReport(reportId);
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "Report rejected successfully");
        return HttpResponse.ok(response);
    }
}
