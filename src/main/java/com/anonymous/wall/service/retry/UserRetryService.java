package com.anonymous.wall.service.retry;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.service.base.UserService;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.UUID;

/**
 * User retry wrapper.
 */
@Singleton
public class UserRetryService {
    private final UserService userService;

    public UserRetryService(UserService userService) {
        this.userService = userService;
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Optional<UserEntity> findById(UUID userId) {
        return userService.findById(userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Optional<UserEntity> findByEmail(String email) {
        return userService.findByEmail(email);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public UserEntity updateProfileName(UUID userId, String profileName) {
        return userService.updateProfileName(userId, profileName);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public UserEntity update(UserEntity user) {
        return userService.update(user);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public UserEntity save(UserEntity user) {
        return userService.save(user);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public boolean isUserBlocked(UUID userId) {
        return userService.isUserBlocked(userId);
    }
}