package com.anonymous.wall.admin.controller;

import com.anonymous.wall.admin.service.AdminUserService;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.AdminUserDTO;
import com.anonymous.wall.model.AdminUserDTORole;
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
import java.time.ZonedDateTime;
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
     * GET /admin/users - List all users with pagination
     */
    @Get
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getAllUsers(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            HttpRequest<?> request) {
        
        log.info("Admin fetching all users - page: {}, limit: {}", page, limit);
        
        // Validate pagination parameters
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        // Create Pageable (0-based indexing)
        Pageable pageable = Pageable.from(page - 1, limit);
        
        // Fetch users
        Page<UserEntity> usersPage = adminUserService.getAllUsers(pageable);
        
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
}
