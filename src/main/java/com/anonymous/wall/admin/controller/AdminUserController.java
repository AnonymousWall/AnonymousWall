package com.anonymous.wall.admin.controller;

import com.anonymous.wall.admin.service.AdminUserService;
import com.anonymous.wall.admin.service.AdminPostService;
import com.anonymous.wall.admin.service.AdminCommentService;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.model.AdminUserDTO;
import com.anonymous.wall.model.AdminUserDTORole;
import com.anonymous.wall.model.AdminPostDTO;
import com.anonymous.wall.model.AdminPostDTOWall;
import com.anonymous.wall.model.AdminCommentDTO;
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

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin controller for user management
 */
@Controller("/api/v1/admin/users")
public class AdminUserController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);
    
    @Inject
    private AdminUserService adminUserService;
    
    @Inject
    private AdminPostService adminPostService;
    
    @Inject
    private AdminCommentService adminCommentService;
    
    /**
     * Convert UserEntity to AdminUserDTO
     */
    private AdminUserDTO mapUserToDTO(UserEntity user) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setProfileName(user.getProfileName());
        dto.setSchoolDomain(user.getSchoolDomain());
        dto.setRole(AdminUserDTORole.fromValue(user.getRole()));
        dto.setBlocked(user.isBlocked());
        dto.setVerified(user.isVerified());
        dto.setPasswordSet(user.isPasswordSet());
        dto.setReportCount(user.getReportCount());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
    
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
     * Convert Comment entity to AdminCommentDTO
     */
    private AdminCommentDTO mapCommentToDTO(Comment comment) {
        AdminCommentDTO dto = new AdminCommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPostId());
        dto.setUserId(comment.getUserId());
        dto.setProfileName(comment.getProfileName());
        dto.setText(comment.getText());
        dto.setHidden(comment.isHidden());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
    
    /**
     * GET /admin/users - List all users with pagination
     */
    @Get
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getAllUsers(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue Boolean blocked,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder,
            HttpRequest<?> request) {
        
        log.info("Admin fetching users - page: {}, limit: {}, blocked: {}, sortBy: {}, sortOrder: {}", 
                 page, limit, blocked, sortBy, sortOrder);
        
        // Validate pagination parameters
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        // Create Pageable (0-based indexing)
        Pageable pageable = Pageable.from(page - 1, limit);
        
        // Fetch users with filters and sorting
        Page<UserEntity> usersPage = adminUserService.getAllUsers(pageable, blocked, sortBy, sortOrder);
        
        // Map to DTOs
        List<AdminUserDTO> userDTOs = usersPage.getContent().stream()
                .map(this::mapUserToDTO)
                .collect(Collectors.toList());
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("data", userDTOs);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", usersPage.getTotalSize());
        pagination.put("totalPages", usersPage.getTotalPages());
        response.put("pagination", pagination);
        
        return HttpResponse.ok(response);
    }
    
    /**
     * GET /admin/users/{id} - Get user by ID
     */
    @Get("/{id}")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<AdminUserDTO> getUserById(@PathVariable String id) {
        log.info("Admin fetching user by ID: {}", id);
        
        UUID userId = UUID.fromString(id);
        UserEntity user = adminUserService.getUserById(userId);
        AdminUserDTO dto = mapUserToDTO(user);
        
        return HttpResponse.ok(dto);
    }
    
    /**
     * POST /admin/users/{id}/block - Block a user
     */
    @Post("/{id}/block")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> blockUser(@PathVariable String id) {
        log.info("Admin blocking user: {}", id);
        
        UUID userId = UUID.fromString(id);
        adminUserService.blockUser(userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User blocked successfully");
        
        return HttpResponse.ok(response);
    }
    
    /**
     * POST /admin/users/{id}/unblock - Unblock a user
     */
    @Post("/{id}/unblock")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> unblockUser(@PathVariable String id) {
        log.info("Admin unblocking user: {}", id);
        
        UUID userId = UUID.fromString(id);
        adminUserService.unblockUser(userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User unblocked successfully");
        
        return HttpResponse.ok(response);
    }
    
    /**
     * GET /admin/users/{id}/posts - Get all posts by a specific user
     */
    @Get("/{id}/posts")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getUserPosts(
            @PathVariable String id,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder,
            HttpRequest<?> request) {
        
        log.info("Admin fetching posts for user: {}, page: {}, limit: {}", id, page, limit);
        
        // Validate pagination parameters
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        UUID userId = UUID.fromString(id);
        
        // Create Pageable (0-based indexing)
        Pageable pageable = Pageable.from(page - 1, limit);
        
        // Fetch posts by userId (including hidden posts)
        Page<Post> postsPage = adminPostService.getAllPosts(pageable, userId, null, sortBy, sortOrder);
        
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
     * GET /admin/users/{id}/comments - Get all comments by a specific user
     */
    @Get("/{id}/comments")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getUserComments(
            @PathVariable String id,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder,
            HttpRequest<?> request) {
        
        log.info("Admin fetching comments for user: {}, page: {}, limit: {}", id, page, limit);
        
        // Validate pagination parameters
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        UUID userId = UUID.fromString(id);
        
        // Create Pageable (0-based indexing)
        Pageable pageable = Pageable.from(page - 1, limit);
        
        // Fetch comments by userId (including hidden comments)
        Page<Comment> commentsPage = adminCommentService.getAllComments(pageable, userId, null, sortBy, sortOrder);
        
        // Map to DTOs
        List<AdminCommentDTO> commentDTOs = commentsPage.getContent().stream()
                .map(this::mapCommentToDTO)
                .collect(Collectors.toList());
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("data", commentDTOs);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", commentsPage.getTotalSize());
        pagination.put("totalPages", commentsPage.getTotalPages());
        response.put("pagination", pagination);
        
        return HttpResponse.ok(response);
    }
}
