package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserService {
    /**
     * Find user by ID
     */
    Optional<UserEntity> findById(UUID userId);

    /**
     * Find user by email
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Update user profile name
     */
    UserEntity updateProfileName(UUID userId, String profileName);

    /**
     * Update user entity
     */
    UserEntity update(UserEntity user);

    /**
     * Save user entity
     */
    UserEntity save(UserEntity user);

    /**
     * Check if user is blocked (cached for performance)
     * @param userId The user ID to check
     * @return true if user is blocked, false otherwise
     */
    boolean isUserBlocked(UUID userId);
}
