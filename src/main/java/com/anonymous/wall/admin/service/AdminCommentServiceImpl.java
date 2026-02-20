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
    public Page<Comment> getAllComments(Pageable pageable, UUID userId, UUID parentId, String parentType, Boolean hidden, String sortBy, String sortOrder) {
        log.info("Admin fetching comments - userId={}, parentId={}, parentType={}, hidden={}, sortBy={}, sortOrder={}",
                 userId, parentId, parentType, hidden, sortBy, sortOrder);
        
        boolean isDesc = sortOrder == null || sortOrder.equalsIgnoreCase("desc");

        // Handle parentId + parentType filter (both provided)
        if (parentId != null && parentType != null) {
            if (userId != null && hidden != null) return commentRepository.findByParentTypeAndParentIdAndHidden(parentType, parentId, hidden, pageable);
            if (userId != null) return commentRepository.findByParentTypeAndParentId(parentType, parentId, pageable);
            if (hidden != null) {
                return isDesc ? commentRepository.findByParentTypeAndParentIdOrderByCreatedAtDesc(parentType, parentId, pageable)
                              : commentRepository.findByParentTypeAndParentIdOrderByCreatedAtAsc(parentType, parentId, pageable);
            }
            return isDesc ? commentRepository.findByParentTypeAndParentIdOrderByCreatedAtDesc(parentType, parentId, pageable)
                          : commentRepository.findByParentTypeAndParentIdOrderByCreatedAtAsc(parentType, parentId, pageable);
        }

        // Handle parentId filter
        if (parentId != null) {
            if (userId != null && hidden != null) return commentRepository.findByParentIdAndUserIdAndHidden(parentId, userId, hidden, pageable);
            if (userId != null) return commentRepository.findByParentIdAndUserId(parentId, userId, pageable);
            if (hidden != null) return commentRepository.findByParentIdAndHidden(parentId, hidden, pageable);
            return commentRepository.findByParentId(parentId, pageable);
        }

        // Handle parentType filter
        if (parentType != null) {
            if (userId != null && hidden != null) return commentRepository.findByParentTypeAndUserIdAndHidden(parentType, userId, hidden, pageable);
            if (userId != null) return commentRepository.findByParentTypeAndUserId(parentType, userId, pageable);
            if (hidden != null) return commentRepository.findByParentTypeAndHidden(parentType, hidden, pageable);
            return isDesc ? commentRepository.findByParentTypeOrderByCreatedAtDesc(parentType, pageable)
                          : commentRepository.findByParentTypeOrderByCreatedAtAsc(parentType, pageable);
        }

        // No parentId/parentType filter - use existing logic
        if (userId == null && hidden == null && sortBy == null) {
            return commentRepository.findAll(pageable);
        }
        
        if (userId == null && hidden == null && sortBy != null) {
            switch (sortBy.toLowerCase()) {
                case "createdat":
                    return isDesc ? commentRepository.findAllOrderByCreatedAtDesc(pageable)
                                  : commentRepository.findAllOrderByCreatedAtAsc(pageable);
                case "userid":
                case "author":
                    return isDesc ? commentRepository.findAllOrderByUserIdDesc(pageable)
                                  : commentRepository.findAllOrderByUserIdAsc(pageable);
                default:
                    return commentRepository.findAll(pageable);
            }
        }
        
        if (userId == null && hidden != null) {
            return commentRepository.findByHidden(hidden, pageable);
        } else if (userId != null && hidden == null) {
            return commentRepository.findByUserId(userId, pageable);
        }
        
        return commentRepository.findByUserIdAndHidden(userId, hidden, pageable);
    }
    
    @Override
    public Comment getCommentById(UUID commentId) {
        log.info("Admin fetching comment by id: {}", commentId);
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with ID: " + commentId));
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

    @Override
    public void hideComment(UUID commentId) {
        log.info("Admin hiding comment: {}", commentId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with ID: " + commentId));
        comment.setHidden(true);
        commentRepository.update(comment);
    }

    @Override
    public void unhideComment(UUID commentId) {
        log.info("Admin unhiding comment: {}", commentId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with ID: " + commentId));
        comment.setHidden(false);
        commentRepository.update(comment);
    }
}
