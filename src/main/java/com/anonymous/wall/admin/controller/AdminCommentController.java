package com.anonymous.wall.admin.controller;

import com.anonymous.wall.admin.service.AdminCommentService;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.model.AdminCommentDTO;
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
 * Admin controller for comment moderation
 */
@Controller("/api/v1/admin/comments")
public class AdminCommentController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminCommentController.class);
    
    @Inject
    private AdminCommentService adminCommentService;
    
    /**
     * Convert Comment entity to AdminCommentDTO
     */
    private AdminCommentDTO mapCommentToDTO(Comment comment) {
        AdminCommentDTO dto = new AdminCommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPostId());
        dto.setUserId(comment.getUserId());
        dto.setProfileName(comment.getProfileName());
        dto.setText(comment.getText());
        dto.setHidden(comment.isHidden());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
    
    /**
     * GET /admin/comments - List all comments with pagination
     */
    @Get
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getAllComments(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String userId,
            @Nullable @QueryValue Boolean hidden,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder,
            HttpRequest<?> request) {
        
        log.info("Admin fetching comments - page: {}, limit: {}, userId: {}, hidden: {}, sortBy: {}, sortOrder: {}", 
                 page, limit, userId, hidden, sortBy, sortOrder);
        
        // Validate pagination parameters
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        // Create Pageable (0-based indexing)
        Pageable pageable = Pageable.from(page - 1, limit);

        // Parse userId if provided
        UUID userIdUuid = userId != null ? UUID.fromString(userId) : null;
        
        // Fetch comments with filters and sorting
        Page<Comment> commentsPage = adminCommentService.getAllComments(pageable, userIdUuid, hidden, sortBy, sortOrder);
        
        // Map to DTOs
        List<AdminCommentDTO> commentDTOs = commentsPage.getContent().stream()
                .map(this::mapCommentToDTO)
                .collect(Collectors.toList());
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("data", commentDTOs);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", commentsPage.getTotalSize());
        pagination.put("totalPages", commentsPage.getTotalPages());
        response.put("pagination", pagination);
        
        return HttpResponse.ok(response);
    }
    
    /**
     * GET /admin/comments/{id} - Get a comment by ID
     */
    @Get("/{id}")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<AdminCommentDTO> getCommentById(@PathVariable String id) {
        log.info("Admin fetching comment by id: {}", id);
        
        UUID commentId = UUID.fromString(id);
        Comment comment = adminCommentService.getCommentById(commentId);
        
        AdminCommentDTO dto = mapCommentToDTO(comment);
        
        return HttpResponse.ok(dto);
    }
    
    /**
     * DELETE /admin/comments/{id} - Soft delete a comment
     */
    @Delete("/{id}")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> deleteComment(@PathVariable String id) {
        log.info("Admin deleting comment: {}", id);
        
        UUID commentId = UUID.fromString(id);
        adminCommentService.deleteComment(commentId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Comment deleted successfully");
        
        return HttpResponse.ok(response);
    }
}
