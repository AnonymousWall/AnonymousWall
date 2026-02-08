package com.anonymous.wall.controller;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.service.AdminService;
import com.anonymous.wall.service.AdminServiceImpl;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin controller for managing users
 */
@Controller("/api/v1/admin")
public class AdminController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    
    @Inject
    private AdminService adminService;
    
    /**
     * Check if the authenticated user is an admin
     */
    private boolean isAdmin(HttpRequest<?> request) {
        Optional<Principal> principalOpt = request.getUserPrincipal();
        if (principalOpt.isEmpty()) {
            return false;
        }
        
        Principal principal = principalOpt.get();
        if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;
            Object role = auth.getAttributes().get("role");
            return AdminServiceImpl.ROLE_ADMIN.equals(role);
        }
        
        return false;
    }
    
    /**
     * GET /admin/users
     * List all users with pagination
     */
    @Get("/users")
    @Secured("IS_AUTHENTICATED")
    public HttpResponse<Object> getAllUsers(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            HttpRequest<?> request) {
        try {
            // Check admin role
            if (!isAdmin(request)) {
                log.warn("Non-admin user attempted to access admin endpoint");
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN)
                    .body(error("Access denied. Admin role required."));
            }
            
            log.info("GET /admin/users - Admin listing all users, page={}, limit={}", page, limit);
            
            // Validate pagination
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;
            
            Pageable pageable = Pageable.from(page - 1, limit);
            Page<UserEntity> userPage = adminService.getAllUsers(pageable);
            
            List<Map<String, Object>> users = userPage.getContent().stream()
                    .map(this::mapUserToAdminDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("limit", limit);
            pagination.put("total", userPage.getTotalSize());
            pagination.put("totalPages", userPage.getTotalPages());
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", users);
            response.put("pagination", pagination);
            
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Error getting all users", e);
            return HttpResponse.badRequest(error("Failed to get users"));
        }
    }
    
    /**
     * GET /admin/users/{userId}
     * Get specific user details
     */
    @Get("/users/{userId}")
    @Secured("IS_AUTHENTICATED")
    public HttpResponse<Object> getUser(@PathVariable String userId, HttpRequest<?> request) {
        try {
            // Check admin role
            if (!isAdmin(request)) {
                log.warn("Non-admin user attempted to access admin endpoint");
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN)
                    .body(error("Access denied. Admin role required."));
            }
            
            log.info("GET /admin/users/{} - Admin viewing user details", userId);
            
            UUID userUuid = UUID.fromString(userId);
            Optional<UserEntity> userOpt = adminService.getUserById(userUuid);
            
            if (userOpt.isEmpty()) {
                return HttpResponse.notFound(error("User not found"));
            }
            
            return HttpResponse.ok(mapUserToAdminDTO(userOpt.get()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format: {}", userId);
            return HttpResponse.badRequest(error("Invalid user ID format"));
        } catch (Exception e) {
            log.error("Error getting user", e);
            return HttpResponse.badRequest(error("Failed to get user"));
        }
    }
    
    /**
     * PUT /admin/users/{userId}/block
     * Block a user account
     */
    @Put("/users/{userId}/block")
    @Secured("IS_AUTHENTICATED")
    public HttpResponse<Object> blockUser(@PathVariable String userId, HttpRequest<?> request) {
        try {
            // Check admin role
            if (!isAdmin(request)) {
                log.warn("Non-admin user attempted to access admin endpoint");
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN)
                    .body(error("Access denied. Admin role required."));
            }
            
            log.info("PUT /admin/users/{}/block - Admin blocking user", userId);
            
            UUID userUuid = UUID.fromString(userId);
            adminService.blockUser(userUuid);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User blocked successfully");
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error blocking user: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Error blocking user: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error blocking user", e);
            return HttpResponse.badRequest(error("Failed to block user"));
        }
    }
    
    /**
     * PUT /admin/users/{userId}/unblock
     * Unblock a user account
     */
    @Put("/users/{userId}/unblock")
    @Secured("IS_AUTHENTICATED")
    public HttpResponse<Object> unblockUser(@PathVariable String userId, HttpRequest<?> request) {
        try {
            // Check admin role
            if (!isAdmin(request)) {
                log.warn("Non-admin user attempted to access admin endpoint");
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN)
                    .body(error("Access denied. Admin role required."));
            }
            
            log.info("PUT /admin/users/{}/unblock - Admin unblocking user", userId);
            
            UUID userUuid = UUID.fromString(userId);
            adminService.unblockUser(userUuid);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User unblocked successfully");
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error unblocking user: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Error unblocking user: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error unblocking user", e);
            return HttpResponse.badRequest(error("Failed to unblock user"));
        }
    }
    
    /**
     * DELETE /admin/users/{userId}
     * Soft delete a user account
     */
    @Delete("/users/{userId}")
    @Secured("IS_AUTHENTICATED")
    public HttpResponse<Object> deleteUser(@PathVariable String userId, HttpRequest<?> request) {
        try {
            // Check admin role
            if (!isAdmin(request)) {
                log.warn("Non-admin user attempted to access admin endpoint");
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN)
                    .body(error("Access denied. Admin role required."));
            }
            
            log.info("DELETE /admin/users/{} - Admin deleting user", userId);
            
            UUID userUuid = UUID.fromString(userId);
            adminService.deleteUser(userUuid);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting user: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Error deleting user: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting user", e);
            return HttpResponse.badRequest(error("Failed to delete user"));
        }
    }
    
    // ================= Helper Methods =================
    
    private Map<String, Object> mapUserToAdminDTO(UserEntity user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId().toString());
        dto.put("email", user.getEmail());
        dto.put("profileName", user.getProfileName());
        dto.put("schoolDomain", user.getSchoolDomain());
        dto.put("role", user.getRole());
        dto.put("accountStatus", user.getAccountStatus());
        dto.put("isVerified", user.isVerified());
        dto.put("passwordSet", user.isPasswordSet());
        dto.put("createdAt", user.getCreatedAt());
        return dto;
    }
    
    private Map<String, String> error(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}
