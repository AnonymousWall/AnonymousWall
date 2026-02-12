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
     * Note: When userId or hidden filters are active, custom sorting parameters are not applied.
     * For custom sorting, omit the filter parameters.
     * 
     * @param pageable Pagination parameters
     * @param userId Filter by author user ID (null = all authors)
     * @param hidden Filter by hidden status (null = all comments)
     * @param sortBy Sort field (case-insensitive): "createdAt", "reportCount", "userId"
     * @param sortOrder Sort order (case-insensitive): "asc" or "desc" (default: desc)
     * @return Page of comments matching the criteria
     */
    @Override
    public Page<Comment> getAllComments(Pageable pageable, UUID userId, Boolean hidden, String sortBy, String sortOrder) {
        log.info("Admin fetching comments - userId={}, hidden={}, sortBy={}, sortOrder={}",
                 userId, hidden, sortBy, sortOrder);
        
        // Determine sort order (default to desc)
        boolean isDesc = sortOrder == null || sortOrder.equalsIgnoreCase("desc");
        
        // If userId or hidden filters are specified, use the existing filter methods
        if (userId != null || hidden != null) {
            if (userId == null && hidden != null) {
                return commentRepository.findByHidden(hidden, pageable);
            } else if (userId != null && hidden == null) {
                return commentRepository.findByUserId(userId, pageable);
            } else {
                return commentRepository.findByUserIdAndHidden(userId, hidden, pageable);
            }
        }
        
        // Handle sorting without filtering
        if (sortBy == null) {
            return commentRepository.findAll(pageable);
        }
        
        switch (sortBy.toLowerCase()) {
            case "createdat":
                return isDesc ?
                    commentRepository.findAllOrderByCreatedAtDesc(pageable) :
                    commentRepository.findAllOrderByCreatedAtAsc(pageable);
            
            case "reportcount":
                return isDesc ?
                    commentRepository.findAllOrderByReportCountDesc(pageable) :
                    commentRepository.findAllOrderByReportCountAsc(pageable);
            
            case "userid":
            case "author":
                return commentRepository.findAllOrderByUserId(pageable);
            
            default:
                return commentRepository.findAll(pageable);
        }
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
