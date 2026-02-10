package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.Post;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

/**
 * Service interface for admin post moderation operations
 */
public interface AdminPostService {
    
    /**
     * Get all posts with pagination and optional filters
     */
    Page<Post> getAllPosts(Pageable pageable, UUID userId, Boolean hidden);
    
    /**
     * Soft delete a post (hide it)
     */
    void deletePost(UUID postId);
}
