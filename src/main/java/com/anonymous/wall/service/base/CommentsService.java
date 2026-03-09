package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.model.CommentParentType;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

public interface CommentsService {
    /**
     * Add a comment to a parent entity (post, internship, or marketplace item)
     */
    Comment addComment(CommentParentType parentType, UUID parentId, CreateCommentRequest request, UUID userId);

    /**
     * Get comments for a parent entity with pagination and sorting
     */
    Page<Comment> getCommentsWithPagination(CommentParentType parentType, UUID parentId, Pageable pageable, SortBy sortBy);

    /**
     * Get comments for a parent entity with pagination, sorting, and block filtering.
     * Comments from users that have a block relationship with currentUserId are excluded.
     */
    Page<Comment> getCommentsWithPagination(CommentParentType parentType, UUID parentId, Pageable pageable, SortBy sortBy, UUID currentUserId);

    /**
     * Hide a comment (soft-delete)
     * Only the comment author can hide their own comment
     */
    Comment hideComment(CommentParentType parentType, UUID parentId, UUID commentId, UUID userId);

    /**
     * Unhide a comment (undo soft-delete)
     * Only the comment author can unhide their own comment
     */
    Comment unhideComment(CommentParentType parentType, UUID parentId, UUID commentId, UUID userId);

    /**
     * Get user's own comments with pagination and sorting
     * Hidden comments are excluded (soft-deleted comments are not shown)
     */
    Page<Comment> getUserOwnComments(UUID userId, Pageable pageable, SortBy sortBy);

    /**
     * Report a comment for inappropriate content
     * A user can only report the same comment once
     * Increments the report count for the comment author
     */
    void reportComment(UUID commentId, UUID reporterUserId, String reason);
}
