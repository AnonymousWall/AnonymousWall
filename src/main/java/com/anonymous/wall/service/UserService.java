package com.anonymous.wall.service;

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
}
