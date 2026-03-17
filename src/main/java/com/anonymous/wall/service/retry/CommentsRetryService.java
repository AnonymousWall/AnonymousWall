package com.anonymous.wall.service.retry;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.model.CommentParentType;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.service.base.CommentsService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;

import java.util.UUID;

/**
 * Comments retry wrapper.
 */
@Singleton
public class CommentsRetryService {

    private final CommentsService commentsService;

    public CommentsRetryService(CommentsService commentsService) {
        this.commentsService = commentsService;
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Comment addComment(CommentParentType parentType, UUID parentId, CreateCommentRequest request, UUID userId) {
        return commentsService.addComment(parentType, parentId, request, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Page<Comment> getCommentsWithPagination(CommentParentType parentType, UUID parentId, Pageable pageable, SortBy sortBy) {
        return commentsService.getCommentsWithPagination(parentType, parentId, pageable, sortBy);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Page<Comment> getCommentsWithPagination(CommentParentType parentType, UUID parentId, Pageable pageable, SortBy sortBy, UUID currentUserId) {
        return commentsService.getCommentsWithPagination(parentType, parentId, pageable, sortBy, currentUserId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Comment hideComment(CommentParentType parentType, UUID parentId, UUID commentId, UUID userId) {
        return commentsService.hideComment(parentType, parentId, commentId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Comment unhideComment(CommentParentType parentType, UUID parentId, UUID commentId, UUID userId) {
        return commentsService.unhideComment(parentType, parentId, commentId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Page<Comment> getUserOwnComments(UUID userId, Pageable pageable, SortBy sortBy) {
        return commentsService.getUserOwnComments(userId, pageable, sortBy);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public void reportComment(UUID commentId, UUID reporterUserId, String reason) {
        commentsService.reportComment(commentId, reporterUserId, reason);
    }
}
