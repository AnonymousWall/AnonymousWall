package com.anonymous.wall.service.retry;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.service.base.PostsService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Posts retry wrapper.
 */
@Singleton
public class PostsRetryService {

    private final PostsService postsService;

    public PostsRetryService(PostsService postsService) {
        this.postsService = postsService;
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Post createPost(CreatePostRequest request, UUID userId) {
        return postsService.createPost(request, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Page<Post> getPostsByWall(String wall, Pageable pageable, UUID currentUserId, String schoolDomain, SortBy sortBy) {
        return postsService.getPostsByWall(wall, pageable, currentUserId, schoolDomain, sortBy);
    }

    @Retryable(attempts = "5", delay = "100ms", excludes = IllegalArgumentException.class)
    public Map<String, Object> toggleLikeWithDetails(UUID postId, UUID userId) {
        return postsService.toggleLikeWithDetails(postId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Post getPost(UUID postId, UUID currentUserId) {
        return postsService.getPost(postId, currentUserId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Post hidePost(UUID postId, UUID userId) {
        return postsService.hidePost(postId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Post unhidePost(UUID postId, UUID userId) {
        return postsService.unhidePost(postId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Page<Post> getUserOwnPosts(UUID userId, Pageable pageable, SortBy sortBy) {
        return postsService.getUserOwnPosts(userId, pageable, sortBy);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public void reportPost(UUID postId, UUID reporterUserId, String reason) {
        postsService.reportPost(postId, reporterUserId, reason);
    }
}
