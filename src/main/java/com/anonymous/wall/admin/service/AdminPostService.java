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
     * Get all posts with pagination and optional filters/sorting
     * @param pageable Pagination parameters
     * @param userId Filter by author user ID (null = all authors)
     * @param hidden Filter by hidden status (null = all posts)
     * @param wall Filter by wall type (null = all walls, "campus" = campus posts, "national" = national posts)
     * @param sortBy Sort field: "createdAt", "likeCount", "commentCount", "userId"
     * @param sortOrder Sort order: "asc" or "desc"
     */
    Page<Post> getAllPosts(Pageable pageable, UUID userId, Boolean hidden, String wall, String sortBy, String sortOrder);
    
    /**
     * Soft delete a post (hide it)
     */
    void deletePost(UUID postId);
}
