package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserBlock;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserBlockRepository;
import com.anonymous.wall.service.base.UserService;
import com.anonymous.wall.service.impl.UserBlockServiceImpl;
import io.micronaut.cache.SyncCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("UserBlockServiceImpl Tests")
class UserBlockServiceImplTest {

    private UserBlockServiceImpl userBlockService;
    private UserBlockRepository userBlockRepository;
    private UserService userService;
    private SyncCache<Object> blockSetsCache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userBlockRepository = mock(UserBlockRepository.class);
        userService = mock(UserService.class);
        blockSetsCache = mock(SyncCache.class);
        userBlockService = new UserBlockServiceImpl();
        try {
            var blockRepoField = UserBlockServiceImpl.class.getDeclaredField("userBlockRepository");
            blockRepoField.setAccessible(true);
            blockRepoField.set(userBlockService, userBlockRepository);

            var userServiceField = UserBlockServiceImpl.class.getDeclaredField("userService");
            userServiceField.setAccessible(true);
            userServiceField.set(userBlockService, userService);

            var cacheField = UserBlockServiceImpl.class.getDeclaredField("blockSetsCache");
            cacheField.setAccessible(true);
            cacheField.set(userBlockService, blockSetsCache);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ================= blockUser =================

    @Nested
    @DisplayName("blockUser Tests")
    class BlockUserTests {

        @Test
        @DisplayName("Positive: Should block user successfully and save correct UserBlock")
        void shouldBlockUserSuccessfully() {
            UUID blockerId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();

            when(userService.findById(targetId)).thenReturn(Optional.of(createTestUser(targetId)));
            when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId)).thenReturn(false);
            when(userBlockRepository.save(any(UserBlock.class))).thenAnswer(i -> i.getArgument(0));

            assertDoesNotThrow(() -> userBlockService.blockUser(blockerId, targetId));

            ArgumentCaptor<UserBlock> captor = ArgumentCaptor.forClass(UserBlock.class);
            verify(userBlockRepository, times(1)).save(captor.capture());
            assertEquals(blockerId, captor.getValue().getBlockerId());
            assertEquals(targetId, captor.getValue().getBlockedId());
        }

        @Test
        @DisplayName("Positive: Should invalidate cache for both blocker and target on block")
        void shouldInvalidateCacheForBothUsersOnBlock() {
            UUID blockerId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();

            when(userService.findById(targetId)).thenReturn(Optional.of(createTestUser(targetId)));
            when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId)).thenReturn(false);
            when(userBlockRepository.save(any(UserBlock.class))).thenAnswer(i -> i.getArgument(0));

            userBlockService.blockUser(blockerId, targetId);

            verify(blockSetsCache).invalidate(blockerId);
            verify(blockSetsCache).invalidate(targetId);
        }

        @Test
        @DisplayName("Negative: Should throw when blocking self")
        void shouldThrowWhenBlockingSelf() {
            UUID userId = UUID.randomUUID();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userBlockService.blockUser(userId, userId));

            assertEquals("You cannot block yourself", ex.getMessage());
            verifyNoInteractions(userService);
            verify(userBlockRepository, never()).save(any());
        }

        @Test
        @DisplayName("Negative: Should throw when target user not found and not interact with block repo")
        void shouldThrowWhenTargetNotFound() {
            UUID blockerId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            when(userService.findById(targetId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userBlockService.blockUser(blockerId, targetId));

            assertEquals("Target user not found", ex.getMessage());
            verify(userBlockRepository, never()).save(any());
            verify(userBlockRepository, never()).existsByBlockerIdAndBlockedId(any(), any());
        }

        @Test
        @DisplayName("Edge: Should throw when user is already blocked")
        void shouldThrowWhenAlreadyBlocked() {
            UUID blockerId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            when(userService.findById(targetId)).thenReturn(Optional.of(createTestUser(targetId)));
            when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId)).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userBlockService.blockUser(blockerId, targetId));

            assertEquals("User is already blocked", ex.getMessage());
            verify(userBlockRepository, never()).save(any());
        }
    }

    // ================= unblockUser =================

    @Nested
    @DisplayName("unblockUser Tests")
    class UnblockUserTests {

        @Test
        @DisplayName("Positive: Should unblock user successfully")
        void shouldUnblockUserSuccessfully() {
            UUID blockerId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            when(userService.findById(targetId)).thenReturn(Optional.of(createTestUser(targetId)));
            when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId)).thenReturn(true);

            assertDoesNotThrow(() -> userBlockService.unblockUser(blockerId, targetId));
            verify(userBlockRepository, times(1)).deleteByBlockerIdAndBlockedId(blockerId, targetId);
        }

        @Test
        @DisplayName("Positive: Should invalidate cache for both blocker and target on unblock")
        void shouldInvalidateCacheForBothUsersOnUnblock() {
            UUID blockerId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            when(userService.findById(targetId)).thenReturn(Optional.of(createTestUser(targetId)));
            when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId)).thenReturn(true);

            userBlockService.unblockUser(blockerId, targetId);

            verify(blockSetsCache).invalidate(blockerId);
            verify(blockSetsCache).invalidate(targetId);
        }

        @Test
        @DisplayName("Negative: Should throw when target not found and not interact with block repo")
        void shouldThrowWhenTargetNotFound() {
            UUID blockerId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            when(userService.findById(targetId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userBlockService.unblockUser(blockerId, targetId));

            assertEquals("Target user not found", ex.getMessage());
            verify(userBlockRepository, never()).deleteByBlockerIdAndBlockedId(any(), any());
        }

        @Test
        @DisplayName("Negative: Should throw when user is not blocked")
        void shouldThrowWhenNotBlocked() {
            UUID blockerId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            when(userService.findById(targetId)).thenReturn(Optional.of(createTestUser(targetId)));
            when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId)).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userBlockService.unblockUser(blockerId, targetId));

            assertEquals("User is not blocked", ex.getMessage());
            verify(userBlockRepository, never()).deleteByBlockerIdAndBlockedId(any(), any());
        }
    }

    // ================= isBlocking =================

    @Nested
    @DisplayName("isBlocking Tests")
    class IsBlockingTests {

        @Test
        @DisplayName("Positive: Should return true when blocking")
        void shouldReturnTrueWhenBlocking() {
            UUID blockerId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId)).thenReturn(true);

            assertTrue(userBlockService.isBlocking(blockerId, targetId));
        }

        @Test
        @DisplayName("Negative: Should return false when not blocking")
        void shouldReturnFalseWhenNotBlocking() {
            UUID blockerId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId)).thenReturn(false);

            assertFalse(userBlockService.isBlocking(blockerId, targetId));
        }

        @Test
        @DisplayName("Edge: isBlocking is directional — A blocks B does not imply B blocks A")
        void isBlockingIsDirectional() {
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();
            when(userBlockRepository.existsByBlockerIdAndBlockedId(userA, userB)).thenReturn(true);
            when(userBlockRepository.existsByBlockerIdAndBlockedId(userB, userA)).thenReturn(false);

            assertTrue(userBlockService.isBlocking(userA, userB));
            assertFalse(userBlockService.isBlocking(userB, userA));
        }
    }

    // ================= isBlockedInAnyDirection =================

    @Nested
    @DisplayName("isBlockedInAnyDirection Tests")
    class IsBlockedInAnyDirectionTests {

        @Test
        @DisplayName("Positive: Should return true when A blocked B")
        void shouldReturnTrueWhenABlockedB() {
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();
            when(userBlockRepository.findByBlockerId(userA)).thenReturn(List.of(new UserBlock(userA, userB)));
            when(userBlockRepository.findByBlockedId(userA)).thenReturn(List.of());

            assertTrue(userBlockService.isBlockedInAnyDirection(userA, userB));
        }

        @Test
        @DisplayName("Positive: Should return true when B blocked A")
        void shouldReturnTrueWhenBBlockedA() {
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();
            when(userBlockRepository.findByBlockerId(userA)).thenReturn(List.of());
            when(userBlockRepository.findByBlockedId(userA)).thenReturn(List.of(new UserBlock(userB, userA)));

            assertTrue(userBlockService.isBlockedInAnyDirection(userA, userB));
        }

        @Test
        @DisplayName("Negative: Should return false when no block exists")
        void shouldReturnFalseWhenNoBlock() {
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();
            when(userBlockRepository.findByBlockerId(userA)).thenReturn(List.of());
            when(userBlockRepository.findByBlockedId(userA)).thenReturn(List.of());

            assertFalse(userBlockService.isBlockedInAnyDirection(userA, userB));
        }

        @Test
        @DisplayName("Edge: Should return false when blocks exist only for an unrelated third user")
        void shouldReturnFalseForUnrelatedThirdUser() {
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();
            UUID userC = UUID.randomUUID();
            when(userBlockRepository.findByBlockerId(userA)).thenReturn(List.of(new UserBlock(userA, userC)));
            when(userBlockRepository.findByBlockedId(userA)).thenReturn(List.of());

            assertFalse(userBlockService.isBlockedInAnyDirection(userA, userB));
        }
    }

    // ================= getBlockedUserIds =================

    @Nested
    @DisplayName("getBlockedUserIds Tests")
    class GetBlockedUserIdsTests {

        @Test
        @DisplayName("Positive: Should return set of blocked user IDs")
        void shouldReturnBlockedUserIds() {
            UUID blockerId = UUID.randomUUID();
            UUID blocked1 = UUID.randomUUID();
            UUID blocked2 = UUID.randomUUID();
            when(userBlockRepository.findByBlockerId(blockerId))
                    .thenReturn(List.of(new UserBlock(blockerId, blocked1), new UserBlock(blockerId, blocked2)));

            Set<UUID> result = userBlockService.getBlockedUserIds(blockerId);

            assertEquals(2, result.size());
            assertTrue(result.contains(blocked1));
            assertTrue(result.contains(blocked2));
        }

        @Test
        @DisplayName("Edge: Should return empty set when user has blocked nobody")
        void shouldReturnEmptySetWhenNoBlocks() {
            UUID blockerId = UUID.randomUUID();
            when(userBlockRepository.findByBlockerId(blockerId)).thenReturn(List.of());

            Set<UUID> result = userBlockService.getBlockedUserIds(blockerId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Edge: Returned set should not include the blocker's own ID")
        void shouldNotIncludeBlockerInResult() {
            UUID blockerId = UUID.randomUUID();
            UUID blocked = UUID.randomUUID();
            when(userBlockRepository.findByBlockerId(blockerId))
                    .thenReturn(List.of(new UserBlock(blockerId, blocked)));

            Set<UUID> result = userBlockService.getBlockedUserIds(blockerId);

            assertFalse(result.contains(blockerId));
        }
    }

    // ================= getCombinedBlockedUserIds =================

    @Nested
    @DisplayName("getCombinedBlockedUserIds Tests")
    class GetCombinedBlockedUserIdsTests {

        @Test
        @DisplayName("Positive: Should return union of outgoing and incoming blocks")
        void shouldReturnCombinedBlockedUserIds() {
            UUID userId = UUID.randomUUID();
            UUID blockedByUser = UUID.randomUUID();
            UUID blockerOfUser = UUID.randomUUID();
            when(userBlockRepository.findByBlockerId(userId))
                    .thenReturn(List.of(new UserBlock(userId, blockedByUser)));
            when(userBlockRepository.findByBlockedId(userId))
                    .thenReturn(List.of(new UserBlock(blockerOfUser, userId)));

            Set<UUID> result = userBlockService.getCombinedBlockedUserIds(userId);

            assertEquals(2, result.size());
            assertTrue(result.contains(blockedByUser));
            assertTrue(result.contains(blockerOfUser));
        }

        @Test
        @DisplayName("Edge: Should return empty set when no blocks in either direction")
        void shouldReturnEmptyWhenNoBlocks() {
            UUID userId = UUID.randomUUID();
            when(userBlockRepository.findByBlockerId(userId)).thenReturn(List.of());
            when(userBlockRepository.findByBlockedId(userId)).thenReturn(List.of());

            Set<UUID> result = userBlockService.getCombinedBlockedUserIds(userId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Edge: Should deduplicate when the same user appears in both outgoing and incoming (mutual block)")
        void shouldDeduplicateOnMutualBlock() {
            UUID userId = UUID.randomUUID();
            UUID otherUser = UUID.randomUUID();
            when(userBlockRepository.findByBlockerId(userId))
                    .thenReturn(List.of(new UserBlock(userId, otherUser)));
            when(userBlockRepository.findByBlockedId(userId))
                    .thenReturn(List.of(new UserBlock(otherUser, userId)));

            Set<UUID> result = userBlockService.getCombinedBlockedUserIds(userId);

            assertEquals(1, result.size(), "Mutual block must not duplicate the other user's ID");
            assertTrue(result.contains(otherUser));
        }
    }

    // ================= getBlockList =================

    @Nested
    @DisplayName("getBlockList Tests")
    class GetBlockListTests {

        @Test
        @DisplayName("Positive: Should return list of UserBlock records")
        void shouldReturnBlockList() {
            UUID blockerId = UUID.randomUUID();
            UUID target1 = UUID.randomUUID();
            UUID target2 = UUID.randomUUID();
            List<UserBlock> blocks = List.of(new UserBlock(blockerId, target1), new UserBlock(blockerId, target2));
            when(userBlockRepository.findByBlockerId(blockerId)).thenReturn(blocks);

            List<UserBlock> result = userBlockService.getBlockList(blockerId);

            assertEquals(2, result.size());
            verify(userBlockRepository, times(1)).findByBlockerId(blockerId);
        }

        @Test
        @DisplayName("Positive: Each UserBlock contains the correct blockedId for profile name lookup")
        void shouldContainCorrectBlockedIdForProfileNameLookup() {
            UUID blockerId = UUID.randomUUID();
            UUID target1 = UUID.randomUUID();
            UUID target2 = UUID.randomUUID();
            UserBlock b1 = new UserBlock(blockerId, target1);
            UserBlock b2 = new UserBlock(blockerId, target2);
            when(userBlockRepository.findByBlockerId(blockerId)).thenReturn(List.of(b1, b2));

            List<UserBlock> result = userBlockService.getBlockList(blockerId);

            assertEquals(2, result.size());
            assertEquals(target1, result.get(0).getBlockedId());
            assertEquals(target2, result.get(1).getBlockedId());
        }

        @Test
        @DisplayName("Edge: Should return empty list when user has no blocks")
        void shouldReturnEmptyListWhenNoBlocks() {
            UUID blockerId = UUID.randomUUID();
            when(userBlockRepository.findByBlockerId(blockerId)).thenReturn(List.of());

            List<UserBlock> result = userBlockService.getBlockList(blockerId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ================= Helper =================

    private UserEntity createTestUser(UUID id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("user" + id + "@harvard.edu");
        user.setSchoolDomain("harvard.edu");
        user.setProfileName("Anonymous");
        user.setVerified(true);
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }
}