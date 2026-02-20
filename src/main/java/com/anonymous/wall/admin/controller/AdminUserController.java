package com.anonymous.wall.admin.controller;

import com.anonymous.wall.admin.service.AdminUserService;
import com.anonymous.wall.admin.service.AdminPostService;
import com.anonymous.wall.admin.service.AdminCommentService;
import com.anonymous.wall.admin.service.AdminInternshipService;
import com.anonymous.wall.admin.service.AdminMarketplaceService;
import com.anonymous.wall.admin.service.AdminChatService;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.model.AdminUserDTO;
import com.anonymous.wall.model.AdminUserDTORole;
import com.anonymous.wall.model.AdminPostDTO;
import com.anonymous.wall.model.AdminPostDTOWall;
import com.anonymous.wall.model.AdminCommentDTO;
import com.anonymous.wall.model.AdminInternshipDTO;
import com.anonymous.wall.model.AdminMarketplaceDTO;
import com.anonymous.wall.model.AdminConversationDTO;
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

import java.math.BigDecimal;
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

    @Inject
    private AdminInternshipService adminInternshipService;

    @Inject
    private AdminMarketplaceService adminMarketplaceService;

    @Inject
    private AdminChatService adminChatService;
    
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
    
    private AdminCommentDTO mapCommentToDTO(Comment comment) {
        AdminCommentDTO dto = new AdminCommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getParentId());
        dto.setUserId(comment.getUserId());
        dto.setProfileName(comment.getProfileName());
        dto.setText(comment.getText());
        dto.setHidden(comment.isHidden());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }

    private AdminInternshipDTO mapInternshipToDTO(Internship internship) {
        AdminInternshipDTO dto = new AdminInternshipDTO();
        dto.setId(internship.getId());
        dto.setUserId(internship.getUserId());
        dto.setProfileName(internship.getProfileName());
        dto.setCompany(internship.getCompany());
        dto.setRole(internship.getRole());
        dto.setSalary(internship.getSalary());
        dto.setLocation(internship.getLocation());
        dto.setDescription(internship.getDescription());
        dto.setDeadline(internship.getDeadline());
        dto.setWall(AdminPostDTOWall.fromValue(internship.getWall()));
        dto.setSchoolDomain(internship.getSchoolDomain());
        dto.setCommentCount(internship.getCommentCount());
        dto.setHidden(internship.isHidden());
        dto.setCreatedAt(internship.getCreatedAt());
        dto.setUpdatedAt(internship.getUpdatedAt());
        return dto;
    }

    private AdminMarketplaceDTO mapMarketplaceToDTO(MarketplaceItem item) {
        AdminMarketplaceDTO dto = new AdminMarketplaceDTO();
        dto.setId(item.getId());
        dto.setUserId(item.getUserId());
        dto.setProfileName(item.getProfileName());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setPrice(item.getPrice());
        dto.setCategory(item.getCategory());
        dto.setCondition(item.getCondition());
        dto.setSold(item.isSold());
        dto.setWall(AdminPostDTOWall.fromValue(item.getWall()));
        dto.setSchoolDomain(item.getSchoolDomain());
        dto.setCommentCount(item.getCommentCount());
        dto.setHidden(item.isHidden());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }
    
    @Get
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getAllUsers(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue Boolean blocked,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder,
            HttpRequest<?> request) {
        
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        Pageable pageable = Pageable.from(page - 1, limit);
        Page<UserEntity> usersPage = adminUserService.getAllUsers(pageable, blocked, sortBy, sortOrder);
        
        List<AdminUserDTO> userDTOs = usersPage.getContent().stream()
                .map(this::mapUserToDTO)
                .collect(Collectors.toList());
        
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
    
    @Get("/{id}")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<AdminUserDTO> getUserById(@PathVariable String id) {
        UUID userId = UUID.fromString(id);
        UserEntity user = adminUserService.getUserById(userId);
        return HttpResponse.ok(mapUserToDTO(user));
    }
    
    @Put("/{id}/block")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> blockUser(@PathVariable String id) {
        log.info("Admin blocking user: {}", id);
        UUID userId = UUID.fromString(id);
        adminUserService.blockUser(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User blocked successfully");
        return HttpResponse.ok(response);
    }
    
    @Put("/{id}/unblock")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> unblockUser(@PathVariable String id) {
        log.info("Admin unblocking user: {}", id);
        UUID userId = UUID.fromString(id);
        adminUserService.unblockUser(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User unblocked successfully");
        return HttpResponse.ok(response);
    }
    
    @Get("/{id}/posts")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getUserPosts(
            @PathVariable String id,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder,
            HttpRequest<?> request) {
        
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        UUID userId = UUID.fromString(id);
        Pageable pageable = Pageable.from(page - 1, limit);
        Page<Post> postsPage = adminPostService.getAllPosts(pageable, userId, null, sortBy, sortOrder);
        
        List<AdminPostDTO> postDTOs = postsPage.getContent().stream()
                .map(this::mapPostToDTO)
                .collect(Collectors.toList());
        
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
    
    @Get("/{id}/comments")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getUserComments(
            @PathVariable String id,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder,
            HttpRequest<?> request) {
        
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        UUID userId = UUID.fromString(id);
        Pageable pageable = Pageable.from(page - 1, limit);
        Page<Comment> commentsPage = adminCommentService.getAllComments(pageable, userId, null, sortBy, sortOrder);
        
        List<AdminCommentDTO> commentDTOs = commentsPage.getContent().stream()
                .map(this::mapCommentToDTO)
                .collect(Collectors.toList());
        
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

    @Get("/{id}/internships")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getUserInternships(
            @PathVariable String id,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit) {

        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;

        UUID userId = UUID.fromString(id);
        Pageable pageable = Pageable.from(page - 1, limit);
        Page<Internship> internshipsPage = adminInternshipService.getInternshipsByUserId(userId, pageable);

        List<AdminInternshipDTO> dtos = internshipsPage.getContent().stream()
                .map(this::mapInternshipToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", dtos);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", internshipsPage.getTotalSize());
        pagination.put("totalPages", internshipsPage.getTotalPages());
        response.put("pagination", pagination);

        return HttpResponse.ok(response);
    }

    @Get("/{id}/marketplaces")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getUserMarketplaces(
            @PathVariable String id,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit) {

        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;

        UUID userId = UUID.fromString(id);
        Pageable pageable = Pageable.from(page - 1, limit);
        Page<MarketplaceItem> itemsPage = adminMarketplaceService.getMarketplacesByUserId(userId, pageable);

        List<AdminMarketplaceDTO> dtos = itemsPage.getContent().stream()
                .map(this::mapMarketplaceToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", dtos);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", itemsPage.getTotalSize());
        pagination.put("totalPages", itemsPage.getTotalPages());
        response.put("pagination", pagination);

        return HttpResponse.ok(response);
    }

    @Get("/{id}/conversations")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getUserConversations(
            @PathVariable String id,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit) {

        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;

        UUID userId = UUID.fromString(id);
        Pageable pageable = Pageable.from(page - 1, limit);
        Page<AdminConversationDTO> convsPage = adminChatService.getAllConversations(pageable, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("data", convsPage.getContent());

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", convsPage.getTotalSize());
        pagination.put("totalPages", convsPage.getTotalPages());
        response.put("pagination", pagination);

        return HttpResponse.ok(response);
    }
}
