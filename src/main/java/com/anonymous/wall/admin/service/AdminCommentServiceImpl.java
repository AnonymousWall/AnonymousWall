package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.repository.CommentRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Implementation of admin comment moderation service
 */
@Singleton
public class AdminCommentServiceImpl implements AdminCommentService {
    
    private static final Logger log = LoggerFactory.getLogger(AdminCommentServiceImpl.class);
    
    @Inject
    private CommentRepository commentRepository;
    
    /**
     * Get all comments with pagination and optional filters/sorting.
     * 
     * @param pageable Pagination parameters
     * @param userId Filter by author user ID (null = all authors)
     * @param hidden Filter by hidden status (null = all comments)
     * @param sortBy Sort field (case-insensitive): "createdAt", "userId"
     * @param sortOrder Sort order (case-insensitive): "asc" or "desc" (default: desc)
     * @return Page of comments matching the criteria
     */
    @Override
    public Page<Comment> getAllComments(Pageable pageable, UUID userId, Boolean hidden, String sortBy, String sortOrder) {
        log.info("Admin fetching comments - userId={}, hidden={}, sortBy={}, sortOrder={}",
                 userId, hidden, sortBy, sortOrder);
        
        // Determine sort order (default to desc)
        boolean isDesc = sortOrder == null || sortOrder.equalsIgnoreCase("desc");
        
        // Case 1: No filters, no custom sorting - return all with default pagination
        if (userId == null && hidden == null && sortBy == null) {
            return commentRepository.findAll(pageable);
        }
        
        // Case 2: No filters, but custom sorting specified - use sorting methods
        if (userId == null && hidden == null && sortBy != null) {
            switch (sortBy.toLowerCase()) {
                case "createdat":
                    return isDesc ?
                        commentRepository.findAllOrderByCreatedAtDesc(pageable) :
                        commentRepository.findAllOrderByCreatedAtAsc(pageable);
                
                case "userid":
                case "author":
                    return isDesc ?
                        commentRepository.findAllOrderByUserIdDesc(pageable) :
                        commentRepository.findAllOrderByUserIdAsc(pageable);
                
                default:
                    return commentRepository.findAll(pageable);
            }
        }
        
        // Case 3: Filters specified (with or without sorting)
        // Note: Micronaut Data filter methods (findByHidden, findByUserId, findByUserIdAndHidden)
        // don't support dynamic sorting, so they use database default ordering (typically by id).
        // To add custom sorting with filters, we would need repository methods like:
        // findByHiddenOrderByCreatedAtDesc, findByUserIdOrderByCreatedAtDesc, etc.
        if (userId == null && hidden != null) {
            // Filter by hidden status only
            return commentRepository.findByHidden(hidden, pageable);
        } else if (userId != null && hidden == null) {
            // Filter by userId only
            return commentRepository.findByUserId(userId, pageable);
        }
        
        // Filter by both userId and hidden (both are non-null if we reach here)
        return commentRepository.findByUserIdAndHidden(userId, hidden, pageable);
    }
    
    @Override
    public void deleteComment(UUID commentId) {
        log.info("Admin soft-deleting comment: {}", commentId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with ID: " + commentId));
        
        comment.setHidden(true);
        commentRepository.update(comment);
        log.info("Comment soft-deleted successfully: {}", commentId);
    }
}
