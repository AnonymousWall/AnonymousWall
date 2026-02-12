package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.UserEntity;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

/**
 * Service interface for admin user management operations
 */
public interface AdminUserService {
    
    /**
     * Get all users with pagination and optional filters/sorting
     * @param pageable Pagination parameters
     * @param blocked Filter by blocked status (null = all users)
     * @param sortBy Sort field: "createdAt", "schoolDomain", "reportCount", "postCount", "commentCount"
     * @param sortOrder Sort order: "asc" or "desc"
     */
    Page<UserEntity> getAllUsers(Pageable pageable, Boolean blocked, String sortBy, String sortOrder);
    
    /**
     * Get user by ID
     */
    UserEntity getUserById(UUID userId);
    
    /**
     * Block a user
     */
    void blockUser(UUID userId);
    
    /**
     * Unblock a user
     */
    void unblockUser(UUID userId);
}
