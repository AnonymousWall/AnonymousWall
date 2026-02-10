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
     * Get all comments with pagination
     */
    Page<Comment> getAllComments(Pageable pageable);
    
    /**
     * Soft delete a comment (hide it)
     */
    void deleteComment(UUID commentId);
}
