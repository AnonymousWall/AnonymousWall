package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserBlock;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserBlockRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.UserBlockService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserBlockService that require a real database.
 * Complements the mock-based unit tests in UserBlockServiceImplTest by
 * verifying that the @Transactional deleteBy... call on unblockUser actually
 * executes inside a transaction and persists correctly.
 */
@MicronautTest(transactional = false)
@DisplayName("UserBlockService Integration Tests")
class UserBlockServiceIntegrationTest {

    @Inject
    private UserBlockService userBlockService;

    @Inject
    private UserBlockRepository userBlockRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity userA;
    private UserEntity userB;

    @BeforeEach
    void setUp() {
        userBlockRepository.deleteAll();

        userA = new UserEntity();
        userA.setEmail("blocka" + System.currentTimeMillis() + "@harvard.edu");
        userA.setSchoolDomain("harvard.edu");
        userA.setVerified(true);
        userA.setPasswordSet(true);
        userA = userRepository.save(userA);

        userB = new UserEntity();
        userB.setEmail("blockb" + System.currentTimeMillis() + "@harvard.edu");
        userB.setSchoolDomain("harvard.edu");
        userB.setVerified(true);
        userB.setPasswordSet(true);
        userB = userRepository.save(userB);
    }

    @AfterEach
    void tearDown() {
        userBlockRepository.deleteAll();
    }

    // ================= blockUser =================

    @Nested
    @DisplayName("blockUser — DB integration")
    class BlockUserIntegrationTests {

        @Test
        @DisplayName("Block is persisted in the database")
        void shouldPersistBlockRecord() {
            userBlockService.blockUser(userA.getId(), userB.getId());

            assertTrue(userBlockRepository.existsByBlockerIdAndBlockedId(userA.getId(), userB.getId()),
                    "A UserBlock row must exist after blockUser()");
        }

        @Test
        @DisplayName("Blocking the same user twice throws and leaves exactly one row")
        void shouldLeaveExactlyOneRowOnDoubleBlock() {
            userBlockService.blockUser(userA.getId(), userB.getId());

            assertThrows(IllegalArgumentException.class,
                    () -> userBlockService.blockUser(userA.getId(), userB.getId()));

            long count = userBlockRepository.findByBlockerId(userA.getId()).stream()
                    .filter(b -> b.getBlockedId().equals(userB.getId()))
                    .count();
            assertEquals(1L, count, "Only one block row must exist after a double-block attempt");
        }
    }

    // ================= unblockUser =================

    @Nested
    @DisplayName("unblockUser — DB integration")
    class UnblockUserIntegrationTests {

        @Test
        @DisplayName("Block row is deleted after unblockUser")
        void shouldDeleteBlockRowInDatabase() {
            userBlockService.blockUser(userA.getId(), userB.getId());
            assertTrue(userBlockRepository.existsByBlockerIdAndBlockedId(userA.getId(), userB.getId()));

            userBlockService.unblockUser(userA.getId(), userB.getId());

            assertFalse(userBlockRepository.existsByBlockerIdAndBlockedId(userA.getId(), userB.getId()),
                    "Block row must be removed from the database after unblockUser()");
        }

        @Test
        @DisplayName("Unblocking a non-blocked user throws and leaves the DB unchanged")
        void shouldThrowAndLeaveDbUnchangedWhenNotBlocked() {
            long countBefore = userBlockRepository.findByBlockerId(userA.getId()).size();

            assertThrows(IllegalArgumentException.class,
                    () -> userBlockService.unblockUser(userA.getId(), userB.getId()));

            long countAfter = userBlockRepository.findByBlockerId(userA.getId()).size();
            assertEquals(countBefore, countAfter,
                    "Block count must be unchanged after a failed unblock attempt");
        }

        @Test
        @DisplayName("Block row is gone and not re-readable after unblockUser")
        void shouldMakeBlockRowGoneAfterUnblock() {
            userBlockService.blockUser(userA.getId(), userB.getId());
            userBlockService.unblockUser(userA.getId(), userB.getId());

            // Direct repository query must confirm the row is gone
            Optional<UserBlock> gone = userBlockRepository.findByBlockerId(userA.getId())
                    .stream()
                    .filter(b -> b.getBlockedId().equals(userB.getId()))
                    .findFirst();
            assertTrue(gone.isEmpty(), "No UserBlock row must remain after unblock");
        }

        @Test
        @DisplayName("Block and unblock can be performed repeatedly")
        void shouldSupportRepeatBlockUnblockCycles() {
            // First cycle
            userBlockService.blockUser(userA.getId(), userB.getId());
            userBlockService.unblockUser(userA.getId(), userB.getId());
            assertFalse(userBlockRepository.existsByBlockerIdAndBlockedId(userA.getId(), userB.getId()));

            // Second cycle — must succeed without any stale state
            userBlockService.blockUser(userA.getId(), userB.getId());
            assertTrue(userBlockRepository.existsByBlockerIdAndBlockedId(userA.getId(), userB.getId()));
            userBlockService.unblockUser(userA.getId(), userB.getId());
            assertFalse(userBlockRepository.existsByBlockerIdAndBlockedId(userA.getId(), userB.getId()));
        }
    }
}
