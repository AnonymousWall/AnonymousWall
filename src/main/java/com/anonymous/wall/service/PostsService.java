package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.List;
import java.util.UUID;

public interface PostsService {
    /**
     * Create a new post
     */
    Post createPost(CreatePostRequest request, UUID userId);

    /**
     * Get posts by wall type with pagination
     */
    Page<Post> getPostsByWall(String wall, Pageable pageable, UUID currentUserId);

    /**
     * Get posts by wall type with pagination and sorting
     */
    Page<Post> getPostsByWall(String wall, Pageable pageable, UUID currentUserId, SortBy sortBy);

    /**
     * Add a comment to a post
     */
    Comment addComment(Long postId, CreateCommentRequest request, UUID userId);

    /**
     * Get all comments for a post
     */
    List<Comment> getComments(Long postId);

    /**
     * Get comments for a post with pagination
     */
    Page<Comment> getCommentsWithPagination(Long postId, Pageable pageable);

    /**
     * Get comments for a post with pagination and sorting
     */
    Page<Comment> getCommentsWithPagination(Long postId, Pageable pageable, SortBy sortBy);

    /**
     * Toggle like on a post (like if not liked, unlike if already liked)
     * Returns true if post is now liked, false if unliked
     */
    boolean toggleLike(Long postId, UUID userId);

    /**
     * Get a single post with like/comment counts
     */
    Post getPost(Long postId, UUID currentUserId);

    /**
     * Hide a comment (soft-delete)
     * Only the comment author can hide their own comment
     */
    Comment hideComment(Long postId, Long commentId, UUID userId);

    /**
     * Unhide a comment (undo soft-delete)
     * Only the comment author can unhide their own comment
     */
    Comment unhideComment(Long postId, Long commentId, UUID userId);

    /**
     * Hide a post (soft-delete)
     * Only the post author can hide their own post
     * When a post is hidden, all its comments are also hidden
     */
    Post hidePost(Long postId, UUID userId);

    /**
     * Unhide a post (undo soft-delete)
     * Only the post author can unhide their own post
     * When a post is unhidden, all its previously hidden comments are restored
     */
    Post unhidePost(Long postId, UUID userId);
}
