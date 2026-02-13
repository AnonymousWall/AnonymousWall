package com.anonymous.wall.admin.controller;

import com.anonymous.wall.admin.service.AdminPostService;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.model.AdminPostDTO;
import com.anonymous.wall.model.AdminPostDTOWall;
import com.anonymous.wall.model.SortBy;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin controller for post moderation
 */
@Controller("/api/v1/admin/posts")
public class AdminPostController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminPostController.class);
    
    @Inject
    private AdminPostService adminPostService;
    
    /**
     * Convert Post entity to AdminPostDTO
     */
    private AdminPostDTO mapPostToDTO(Post post) {
        AdminPostDTO dto = new AdminPostDTO();
        dto.setId(post.getId());
        dto.setUserId(post.getUserId());
        dto.setProfileName(post.getProfileName());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setWall(AdminPostDTOWall.fromValue(post.getWall()));
        dto.setSchoolDomain(post.getSchoolDomain());
        dto.setLikeCount(post.getLikeCount());
        dto.setCommentCount(post.getCommentCount());
        dto.setHidden(post.isHidden());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        return dto;
    }
    
    /**
     * GET /admin/posts - List all posts with pagination and filters
     */
    @Get
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getAllPosts(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String userId,
            @Nullable @QueryValue Boolean hidden,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder,
            HttpRequest<?> request) {
        
        log.info("Admin fetching posts - page: {}, limit: {}, userId: {}, hidden: {}, sortBy: {}, sortOrder: {}", 
                 page, limit, userId, hidden, sortBy, sortOrder);
        
        // Validate pagination parameters
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        // Create Pageable (0-based indexing)
        Pageable pageable = Pageable.from(page - 1, limit);
        
        // Parse userId if provided
        UUID userIdUuid = userId != null ? UUID.fromString(userId) : null;
        
        // Fetch posts with filters and sorting
        Page<Post> postsPage = adminPostService.getAllPosts(pageable, userIdUuid, hidden, sortBy, sortOrder);
        
        // Map to DTOs
        List<AdminPostDTO> postDTOs = postsPage.getContent().stream()
                .map(this::mapPostToDTO)
                .collect(Collectors.toList());
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("data", postDTOs);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", postsPage.getTotalSize());
        pagination.put("totalPages", postsPage.getTotalPages());
        response.put("pagination", pagination);
        
        return HttpResponse.ok(response);
    }
    
    /**
     * GET /admin/posts/{id} - Get a post by ID
     */
    @Get("/{id}")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<AdminPostDTO> getPostById(@PathVariable String id) {
        log.info("Admin fetching post by id: {}", id);
        
        UUID postId = UUID.fromString(id);
        Post post = adminPostService.getPostById(postId);
        
        AdminPostDTO dto = mapPostToDTO(post);
        
        return HttpResponse.ok(dto);
    }
    
    /**
     * DELETE /admin/posts/{id} - Soft delete a post
     */
    @Delete("/{id}")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> deletePost(@PathVariable String id) {
        log.info("Admin deleting post: {}", id);
        
        UUID postId = UUID.fromString(id);
        adminPostService.deletePost(postId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Post deleted successfully");
        
        return HttpResponse.ok(response);
    }
    
    /**
     * GET /admin/posts/by-wall - Get posts by wall with sorting (admin version)
     * Allows admin to filter by wall type (national/campus/all) and sort
     * Does NOT filter by schoolDomain, includes both hidden and non-hidden posts
     */
    @Get("/by-wall")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getPostsByWall(
            @Nullable @QueryValue String wall,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String sortBy,
            HttpRequest<?> request) {
        
        log.info("Admin fetching posts by wall - wall: {}, page: {}, limit: {}, sortBy: {}", 
                 wall, page, limit, sortBy);
        
        // Validate pagination parameters
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        // Create Pageable (0-based indexing)
        Pageable pageable = Pageable.from(page - 1, limit);
        
        // Parse sortBy parameter
        SortBy sort = SortBy.parse(sortBy);
        
        // Fetch posts by wall with sorting
        Page<Post> postsPage = adminPostService.getPostsByWall(wall, pageable, sort);
        
        // Map to DTOs
        List<AdminPostDTO> postDTOs = postsPage.getContent().stream()
                .map(this::mapPostToDTO)
                .collect(Collectors.toList());
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("data", postDTOs);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", postsPage.getTotalSize());
        pagination.put("totalPages", postsPage.getTotalPages());
        response.put("pagination", pagination);
        
        return HttpResponse.ok(response);
    }
}
