package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserBlock;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserBlockService {

    /**
     * Block a user. Validates that the target exists, that it is not a self-block,
     * and that the block does not already exist.
     */
    void blockUser(UUID blockerId, UUID targetUserId);

    /**
     * Unblock a previously blocked user.
     */
    void unblockUser(UUID blockerId, UUID targetUserId);

    /**
     * Returns true if blockerId has blocked targetUserId.
     */
    boolean isBlocking(UUID blockerId, UUID targetUserId);

    /**
     * Returns true if either user has blocked the other.
     */
    boolean isBlockedInAnyDirection(UUID userId1, UUID userId2);

    /**
     * Returns the set of user IDs that the given user has blocked.
     */
    Set<UUID> getBlockedUserIds(UUID userId);

    /**
     * Returns the union of user IDs blocked by the given user AND user IDs that blocked the given user.
     */
    Set<UUID> getCombinedBlockedUserIds(UUID userId);

    /**
     * Returns the list of UserBlock records created by the given user.
     */
    List<UserBlock> getBlockList(UUID blockerId);
}
