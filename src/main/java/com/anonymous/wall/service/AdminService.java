package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for admin operations
 */
public interface AdminService {
    
    /**
     * Get all users with pagination
     */
    Page<UserEntity> getAllUsers(Pageable pageable);
    
    /**
     * Get a specific user by ID
     */
    Optional<UserEntity> getUserById(UUID userId);
    
    /**
     * Block a user account
     */
    void blockUser(UUID userId);
    
    /**
     * Unblock a user account
     */
    void unblockUser(UUID userId);
    
    /**
     * Soft delete a user account
     */
    void deleteUser(UUID userId);
}
