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
    public Page<Comment> getAllComments(Pageable pageable) {
        log.info("Admin fetching all comments with pagination: page={}, size={}", 
                 pageable.getNumber(), pageable.getSize());
        return commentRepository.findAll(pageable);
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
