package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.Comment;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

/**
 * Service interface for admin comment moderation operations
 */
public interface AdminCommentService {
    
    /**
     * Get all comments with pagination and optional filters/sorting
     * @param pageable Pagination parameters
     * @param userId Filter by author user ID (null = all authors)
     * @param hidden Filter by hidden status (null = all comments)
     * @param sortBy Sort field: "createdAt", "userId"
     * @param sortOrder Sort order: "asc" or "desc"
     */
    Page<Comment> getAllComments(Pageable pageable, UUID userId, Boolean hidden, String sortBy, String sortOrder);
    
    Comment getCommentById(UUID commentId);
    
    void deleteComment(UUID commentId);

    void hideComment(UUID commentId);

    void unhideComment(UUID commentId);
}
