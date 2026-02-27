package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserBlock;
import com.anonymous.wall.repository.UserBlockRepository;
import com.anonymous.wall.repository.UserRepository;
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
public class UserBlockServiceImpl implements UserBlockService {

    private static final Logger log = LoggerFactory.getLogger(UserBlockServiceImpl.class);

    @Inject
    private UserBlockRepository userBlockRepository;

    @Inject
    private UserRepository userRepository;

    @Override
    public void blockUser(UUID blockerId, UUID targetUserId) {
        if (blockerId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot block yourself");
        }
        if (userRepository.findById(targetUserId).isEmpty()) {
            throw new IllegalArgumentException("Target user not found");
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId)) {
            throw new IllegalArgumentException("User is already blocked");
        }
        UserBlock block = new UserBlock(blockerId, targetUserId);
        userBlockRepository.save(block);
        log.info("User {} blocked user {}", blockerId, targetUserId);
    }

    @Override
    public void unblockUser(UUID blockerId, UUID targetUserId) {
        if (userRepository.findById(targetUserId).isEmpty()) {
            throw new IllegalArgumentException("Target user not found");
        }
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId)) {
            throw new IllegalArgumentException("User is not blocked");
        }
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, targetUserId);
        log.info("User {} unblocked user {}", blockerId, targetUserId);
    }

    @Override
    public boolean isBlocking(UUID blockerId, UUID targetUserId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId);
    }

    @Override
    public boolean isBlockedInAnyDirection(UUID userId1, UUID userId2) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(userId1, userId2)
                || userBlockRepository.existsByBlockerIdAndBlockedId(userId2, userId1);
    }

    @Override
    public Set<UUID> getBlockedUserIds(UUID userId) {
        return userBlockRepository.findByBlockerId(userId).stream()
                .map(UserBlock::getBlockedId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<UUID> getCombinedBlockedUserIds(UUID userId) {
        Set<UUID> combined = new HashSet<>();
        userBlockRepository.findByBlockerId(userId).stream()
                .map(UserBlock::getBlockedId)
                .forEach(combined::add);
        userBlockRepository.findByBlockedId(userId).stream()
                .map(UserBlock::getBlockerId)
                .forEach(combined::add);
        return combined;
    }

    @Override
    public List<UserBlock> getBlockList(UUID blockerId) {
        return userBlockRepository.findByBlockerId(blockerId);
    }
}
