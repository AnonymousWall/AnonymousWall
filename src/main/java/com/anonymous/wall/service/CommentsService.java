package com.anonymous.wall.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.List;
import java.util.UUID;

public interface CommentsService {
    /**
     * Add a comment to a post
     */
    Comment addComment(UUID postId, CreateCommentRequest request, UUID userId);

    /**
     * Get all comments for a post
     */
    List<Comment> getComments(UUID postId);

    /**
     * Get comments for a post with pagination
     */
    Page<Comment> getCommentsWithPagination(UUID postId, Pageable pageable);

    /**
     * Get comments for a post with pagination and sorting
     */
    Page<Comment> getCommentsWithPagination(UUID postId, Pageable pageable, SortBy sortBy);

    /**
     * Hide a comment (soft-delete)
     * Only the comment author can hide their own comment
     */
    Comment hideComment(UUID postId, UUID commentId, UUID userId);

    /**
     * Unhide a comment (undo soft-delete)
     * Only the comment author can unhide their own comment
     */
    Comment unhideComment(UUID postId, UUID commentId, UUID userId);
}
