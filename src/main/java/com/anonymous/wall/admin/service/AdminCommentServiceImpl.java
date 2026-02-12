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
    
    @Override
    public Page<Comment> getAllComments(Pageable pageable, UUID userId, Boolean hidden) {
        log.info("Admin fetching all comments with pagination: page={}, size={}, userId={}, hidden={}",
                 pageable.getNumber(), pageable.getSize(), userId, hidden);

        if (userId == null && hidden == null) {
            return commentRepository.findAll(pageable);
        } else if (userId != null && hidden == null) {
            return commentRepository.findByUserId(userId, pageable);
        } else if (userId == null && hidden != null) {
            return commentRepository.findByHidden(hidden, pageable);
        } else {
            return commentRepository.findByUserIdAndHidden(userId, hidden, pageable);
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
