package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.CachePut;
import io.micronaut.cache.annotation.Cacheable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for user operations with caching
 * Provides cached access to frequently accessed user data
 */
@Singleton
public class UserCacheService {
    private static final Logger log = LoggerFactory.getLogger(UserCacheService.class);

    @Inject
    private UserRepository userRepository;

    /**
     * Find user by ID with caching
     * Cache key: user:findById:{id}
     * Prevents cache penetration by not caching null results
     */
    @Cacheable(value = "user:findById", parameters = {"id"})
    public Optional<UserEntity> findById(UUID id) {
        log.debug("Finding user by ID: {}", id);
        Optional<UserEntity> user = userRepository.findById(id);
        
        // Don't cache null results to prevent cache penetration
        if (user.isEmpty()) {
            log.debug("User not found by ID: {}, not caching", id);
            return Optional.empty();
        }
        
        log.debug("User found by ID: {}", id);
        return user;
    }

    /**
     * Find user by email with caching
     * Cache key: user:findByEmail:{email}
     * Prevents cache penetration by not caching null results
     */
    @Cacheable(value = "user:findByEmail", parameters = {"email"})
    public Optional<UserEntity> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        Optional<UserEntity> user = userRepository.findByEmail(email);
        
        // Don't cache null results to prevent cache penetration
        if (user.isEmpty()) {
            log.debug("User not found by email: {}, not caching", email);
            return Optional.empty();
        }
        
        log.debug("User found by email: {}", email);
        return user;
    }

    /**
     * Save user and update cache
     * Invalidates both ID and email caches for consistency
     */
    @CachePut(value = "user:findById", parameters = {"user.id"})
    @CacheInvalidate(value = "user:findByEmail", parameters = {"user.email"})
    public UserEntity save(UserEntity user) {
        log.debug("Saving user: {}", user.getEmail());
        UserEntity saved = userRepository.save(user);
        log.info("User saved: id={}, email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    /**
     * Update user and invalidate caches
     * Invalidates both ID and email caches for consistency
     */
    @CachePut(value = "user:findById", parameters = {"user.id"})
    @CacheInvalidate(value = "user:findByEmail", all = true)
    public UserEntity update(UserEntity user) {
        log.debug("Updating user: {}", user.getId());
        UserEntity updated = userRepository.update(user);
        log.info("User updated: id={}, email={}", updated.getId(), updated.getEmail());
        return updated;
    }

    /**
     * Invalidate all user caches for a specific user
     * Use when user data changes in complex ways
     */
    @CacheInvalidate(value = "user:findById", parameters = {"userId"})
    @CacheInvalidate(value = "user:findByEmail", all = true)
    public void invalidateUserCache(UUID userId) {
        log.info("Invalidating all caches for user: {}", userId);
    }
}
