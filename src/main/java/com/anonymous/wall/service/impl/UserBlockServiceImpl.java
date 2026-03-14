package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.UserBlock;
import com.anonymous.wall.repository.UserBlockRepository;
import com.anonymous.wall.service.base.UserBlockService;
import com.anonymous.wall.service.base.UserService;
import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
@CacheConfig("user-block-sets")
public class UserBlockServiceImpl implements UserBlockService {

    private static final Logger log = LoggerFactory.getLogger(UserBlockServiceImpl.class);

    @Inject
    private UserBlockRepository userBlockRepository;

    @Inject
    private UserService userService;

    @Inject
    @jakarta.inject.Named("user-block-sets")
    private io.micronaut.cache.SyncCache<Object> blockSetsCache;

    /**
     * Block a user.
     * Invalidates cache for both blocker and target:
     * - blocker's outgoing block set changed
     * - target's incoming block set changed
     */
    @Override
    @Transactional
    @CacheInvalidate(parameters = {"blockerId"})
    @CacheInvalidate(parameters = {"targetUserId"})
    public void blockUser(UUID blockerId, UUID targetUserId) {
        if (blockerId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot block yourself");
        }
        if (userService.findById(targetUserId).isEmpty()) {
            throw new IllegalArgumentException("Target user not found");
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId)) {
            throw new IllegalArgumentException("User is already blocked");
        }
        UserBlock block = new UserBlock(blockerId, targetUserId);
        userBlockRepository.save(block);
        invalidateBothUsers(blockerId, targetUserId);
        log.info("User {} blocked user {}", blockerId, targetUserId);
    }

    /**
     * Unblock a user.
     * Invalidates cache for both blocker and target.
     */
    @Override
    @Transactional
    @CacheInvalidate(parameters = {"blockerId"})
    @CacheInvalidate(parameters = {"targetUserId"})
    public void unblockUser(UUID blockerId, UUID targetUserId) {
        if (userService.findById(targetUserId).isEmpty()) {
            throw new IllegalArgumentException("Target user not found");
        }
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId)) {
            throw new IllegalArgumentException("User is not blocked");
        }
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, targetUserId);
        invalidateBothUsers(blockerId, targetUserId);
        log.info("User {} unblocked user {}", blockerId, targetUserId);
    }

    private void invalidateBothUsers(UUID userId1, UUID userId2) {
        blockSetsCache.invalidate(userId1);
        blockSetsCache.invalidate(userId2);
        log.debug("Invalidated user-block-sets cache for {} and {}", userId1, userId2);
    }

    /**
     * Check if userId1 has blocked userId2 or vice versa.
     * Not cached directly — callers should prefer getCombinedBlockedUserIds(userId).contains(otherId)
     * to benefit from caching when checking multiple users.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isBlocking(UUID blockerId, UUID targetUserId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId);
    }

    /**
     * Check if a block exists in either direction between two users.
     * Uses getCombinedBlockedUserIds to benefit from caching.
     */
    @Override
    public boolean isBlockedInAnyDirection(UUID userId1, UUID userId2) {
        return getCombinedBlockedUserIds(userId1).contains(userId2);
    }

    /**
     * Get IDs of all users that userId has blocked (outgoing blocks only).
     * Cached by userId — invalidated on blockUser/unblockUser.
     */
    @Override
    @Cacheable(parameters = {"userId"})
    @Transactional(readOnly = true)
    public Set<UUID> getBlockedUserIds(UUID userId) {
        log.debug("getBlockedUserIds cache miss for user {}", userId);
        return userBlockRepository.findByBlockerId(userId).stream()
                .map(UserBlock::getBlockedId)
                .collect(Collectors.toSet());
    }

    /**
     * Get IDs of all users in a block relationship with userId in either direction.
     * This is the primary method used by feed/content filtering — cached aggressively.
     * Cached by userId — invalidated on blockUser/unblockUser for both affected users.
     */
    @Override
    @Cacheable(parameters = {"userId"})
    @Transactional(readOnly = true)
    public Set<UUID> getCombinedBlockedUserIds(UUID userId) {
        log.debug("getCombinedBlockedUserIds cache miss for user {}", userId);
        Set<UUID> combined = new HashSet<>();
        userBlockRepository.findByBlockerId(userId).stream()
                .map(UserBlock::getBlockedId)
                .forEach(combined::add);
        userBlockRepository.findByBlockedId(userId).stream()
                .map(UserBlock::getBlockerId)
                .forEach(combined::add);
        return combined;
    }

    /**
     * Get the full block list for a user (not cached — low frequency, user-facing only).
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserBlock> getBlockList(UUID blockerId) {
        return userBlockRepository.findByBlockerId(blockerId);
    }
}