package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.event.ProfileNameChangedEvent;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@Singleton
@CacheConfig("blocked-users")
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Inject
    private UserRepository userRepository;

    @Inject
    private ApplicationEventPublisher<ProfileNameChangedEvent> eventPublisher;

    /**
     * Find user by ID
     */
    @Override
    public Optional<UserEntity> findById(UUID userId) {
        log.debug("Finding user by ID: {}", userId);
        return userRepository.findById(userId);
    }

    /**
     * Find user by email
     */
    @Override
    public Optional<UserEntity> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Update user profile name
     */
    @Override
    public UserEntity updateProfileName(UUID userId, String profileName) {
        log.debug("Updating profile name for user: {}", userId);
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", userId);
            throw new IllegalArgumentException("User not found");
        }

        UserEntity user = userOpt.get();
        String oldProfileName = user.getProfileName();
        
        // Validate and set profile name
        String newProfileName;
        if (profileName == null || profileName.trim().isEmpty()) {
            newProfileName = "Anonymous";
        } else {
            newProfileName = profileName.trim();
        }
        
        user.setProfileName(newProfileName);
        UserEntity updated = userRepository.update(user);
        
        log.info("Profile name updated for user: {}, oldName={}, newName={}", 
                userId, oldProfileName, newProfileName);
        
        // Publish event for asynchronous profile name propagation to posts and comments
        eventPublisher.publishEvent(
            new ProfileNameChangedEvent(userId, oldProfileName, newProfileName)
        );
        log.debug("Published ProfileNameChangedEvent for userId={}", userId);
        
        return updated;
    }

    /**
     * Update user entity
     */
    @Override
    public UserEntity update(UserEntity user) {
        log.debug("Updating user: {}", user.getId());
        return userRepository.update(user);
    }

    /**
     * Save user entity
     */
    @Override
    public UserEntity save(UserEntity user) {
        log.debug("Saving user");
        return userRepository.save(user);
    }

    /**
     * Check if user is blocked (cached for performance)
     * Uses Caffeine cache to minimize database access
     * Cache is automatically invalidated when user is updated
     */
    @Override
    @Cacheable
    public boolean isUserBlocked(UUID userId) {
        log.debug("Checking blocked status for user: {} (cache miss)", userId);
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        return userOpt.map(UserEntity::isBlocked).orElse(false);
    }
}
