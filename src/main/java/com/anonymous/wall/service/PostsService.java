package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.multipart.CompletedFileUpload;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PostsService {
    /**
     * Create a new post with optional image upload
     */
    Post createPost(CreatePostRequest request, CompletedFileUpload image, UUID userId);

    /**
     * Get posts by wall type with pagination and sorting (optimized with schoolDomain from JWT)
     */
    Page<Post> getPostsByWall(String wall, Pageable pageable, UUID currentUserId, String schoolDomain, SortBy sortBy);

    /**
     * Toggle like on a post with details
     * Returns a map containing:
     * - "liked": boolean indicating if the post is now liked
     * - "likeCount": long indicating the total number of likes on the post
     * This is more efficient than toggleLike() as it avoids an extra query in the controller
     */
    Map<String, Object> toggleLikeWithDetails(UUID postId, UUID userId);

    /**
     * Get a single post with like/comment counts
     */
    Post getPost(UUID postId, UUID currentUserId);

    /**
     * Hide a post (soft-delete)
     * Only the post author can hide their own post
     * When a post is hidden, all its comments are also hidden asynchronously
     */
    Post hidePost(UUID postId, UUID userId);

    /**
     * Unhide a post (undo soft-delete)
     * Only the post author can unhide their own post
     * When a post is unhidden, all its previously hidden comments are restored
     */
    Post unhidePost(UUID postId, UUID userId);

    /**
     * Get user's own posts with pagination and sorting
     * Only returns non-hidden posts
     */
    Page<Post> getUserOwnPosts(UUID userId, Pageable pageable, SortBy sortBy);

    /**
     * Report a post for inappropriate content
     * A user can only report the same post once
     * Increments the report count for the post author
     */
    void reportPost(UUID postId, UUID reporterUserId, String reason);
}
