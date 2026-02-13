package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.model.SortBy;
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
     * @param sortBy Sort field: "createdAt", "likeCount", "commentCount", "userId"
     * @param sortOrder Sort order: "asc" or "desc"
     */
    Page<Post> getAllPosts(Pageable pageable, UUID userId, Boolean hidden, String sortBy, String sortOrder);
    
    /**
     * Get posts by wall with pagination and sorting
     * Admin version: does NOT filter by schoolDomain, includes both hidden and non-hidden posts
     * @param wall Wall type: "national", "campus", or null for all posts
     * @param pageable Pagination parameters
     * @param sortBy Sort type: NEWEST, OLDEST, MOST_LIKED, LEAST_LIKED
     * @return Page of posts matching the criteria
     */
    Page<Post> getPostsByWall(String wall, Pageable pageable, SortBy sortBy);
    
    /**
     * Get a post by its ID
     * @param postId The ID of the post to retrieve
     * @return The post with the specified ID
     */
    Post getPostById(UUID postId);
    
    /**
     * Soft delete a post (hide it)
     */
    void deletePost(UUID postId);
}
