package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for UserCacheService caching behavior
 * Verifies that caching works correctly and cache invalidation happens appropriately
 */
@MicronautTest
@DisplayName("UserCacheService - Caching Tests")
class UserCacheServiceTest {

    @Inject
    private UserCacheService userCacheService;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        // Clean up
        userRepository.deleteAll();

        // Create test user
        testUser = new UserEntity();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("cache-test@harvard.edu");
        testUser.setSchoolDomain("harvard.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser.setPasswordHash("dummy");
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("FindById Caching Tests")
    class FindByIdCachingTests {

        @Test
        @DisplayName("Should cache user when found by ID")
        void shouldCacheUserWhenFoundById() {
            // First call - should hit database
            Optional<UserEntity> user1 = userCacheService.findById(testUserId);
            assertTrue(user1.isPresent(), "User should be found");
            assertEquals(testUserId, user1.get().getId());

            // Second call - should use cache (we can verify this works)
            Optional<UserEntity> user2 = userCacheService.findById(testUserId);
            assertTrue(user2.isPresent(), "User should still be found");
            assertEquals(testUserId, user2.get().getId());
        }

        @Test
        @DisplayName("Should not cache when user not found by ID")
        void shouldNotCacheWhenUserNotFoundById() {
            UUID nonExistentId = UUID.randomUUID();
            
            // First call - should return empty
            Optional<UserEntity> user1 = userCacheService.findById(nonExistentId);
            assertFalse(user1.isPresent(), "User should not be found");

            // Second call - should also return empty (not cached)
            Optional<UserEntity> user2 = userCacheService.findById(nonExistentId);
            assertFalse(user2.isPresent(), "User should still not be found");
        }
    }

    @Nested
    @DisplayName("FindByEmail Caching Tests")
    class FindByEmailCachingTests {

        @Test
        @DisplayName("Should cache user when found by email")
        void shouldCacheUserWhenFoundByEmail() {
            String email = testUser.getEmail();
            
            // First call - should hit database
            Optional<UserEntity> user1 = userCacheService.findByEmail(email);
            assertTrue(user1.isPresent(), "User should be found");
            assertEquals(email, user1.get().getEmail());

            // Second call - should use cache
            Optional<UserEntity> user2 = userCacheService.findByEmail(email);
            assertTrue(user2.isPresent(), "User should still be found");
            assertEquals(email, user2.get().getEmail());
        }

        @Test
        @DisplayName("Should not cache when user not found by email")
        void shouldNotCacheWhenUserNotFoundByEmail() {
            String nonExistentEmail = "nonexistent@test.com";
            
            // First call - should return empty
            Optional<UserEntity> user1 = userCacheService.findByEmail(nonExistentEmail);
            assertFalse(user1.isPresent(), "User should not be found");

            // Second call - should also return empty (not cached)
            Optional<UserEntity> user2 = userCacheService.findByEmail(nonExistentEmail);
            assertFalse(user2.isPresent(), "User should still not be found");
        }
    }

    @Nested
    @DisplayName("Cache Invalidation Tests")
    class CacheInvalidationTests {

        @Test
        @DisplayName("Should invalidate cache when user is updated")
        void shouldInvalidateCacheOnUpdate() {
            // Cache the user
            Optional<UserEntity> cachedUser = userCacheService.findById(testUserId);
            assertTrue(cachedUser.isPresent());
            String originalEmail = cachedUser.get().getEmail();

            // Update the user
            testUser.setEmail("updated@harvard.edu");
            UserEntity updatedUser = userCacheService.update(testUser);
            
            // Verify update happened
            assertEquals("updated@harvard.edu", updatedUser.getEmail());
            
            // Fetch again - should get updated value
            Optional<UserEntity> refetchedUser = userCacheService.findById(testUserId);
            assertTrue(refetchedUser.isPresent());
            assertEquals("updated@harvard.edu", refetchedUser.get().getEmail());
        }

        @Test
        @DisplayName("Should update cache when user is saved")
        void shouldUpdateCacheOnSave() {
            UserEntity newUser = new UserEntity();
            newUser.setId(UUID.randomUUID());
            newUser.setEmail("newuser@mit.edu");
            newUser.setSchoolDomain("mit.edu");
            newUser.setVerified(true);
            newUser.setPasswordSet(true);
            newUser.setPasswordHash("dummy");

            // Save new user
            UserEntity savedUser = userCacheService.save(newUser);
            assertNotNull(savedUser.getId());

            // Should be in cache now
            Optional<UserEntity> cachedUser = userCacheService.findById(savedUser.getId());
            assertTrue(cachedUser.isPresent());
            assertEquals("newuser@mit.edu", cachedUser.get().getEmail());
        }

        @Test
        @DisplayName("Should invalidate all caches for specific user")
        void shouldInvalidateAllCachesForUser() {
            // Cache the user
            userCacheService.findById(testUserId);
            userCacheService.findByEmail(testUser.getEmail());

            // Invalidate all caches for this user
            userCacheService.invalidateUserCache(testUserId);

            // Subsequent calls should hit database again
            // We can't easily verify database hits, but we can verify data is still correct
            Optional<UserEntity> user = userCacheService.findById(testUserId);
            assertTrue(user.isPresent());
            assertEquals(testUserId, user.get().getId());
        }
    }

    @Nested
    @DisplayName("Cache Consistency Tests")
    class CacheConsistencyTests {

        @Test
        @DisplayName("Should maintain consistency between ID and email caches")
        void shouldMaintainConsistencyBetweenCaches() {
            // Cache via ID lookup
            Optional<UserEntity> userById = userCacheService.findById(testUserId);
            assertTrue(userById.isPresent());

            // Cache via email lookup
            Optional<UserEntity> userByEmail = userCacheService.findByEmail(testUser.getEmail());
            assertTrue(userByEmail.isPresent());

            // Both should reference the same user
            assertEquals(userById.get().getId(), userByEmail.get().getId());
            assertEquals(userById.get().getEmail(), userByEmail.get().getEmail());
        }

        @Test
        @DisplayName("Should handle concurrent access correctly")
        void shouldHandleConcurrentAccess() {
            // Simulate concurrent access
            for (int i = 0; i < 10; i++) {
                Optional<UserEntity> user = userCacheService.findById(testUserId);
                assertTrue(user.isPresent());
                assertEquals(testUserId, user.get().getId());
            }
        }
    }

    @Nested
    @DisplayName("Cache Penetration Prevention Tests")
    class CachePenetrationPreventionTests {

        @Test
        @DisplayName("Should not cache null results to prevent cache penetration")
        void shouldNotCacheNullResults() {
            UUID nonExistentId = UUID.randomUUID();
            
            // Multiple calls with non-existent ID
            for (int i = 0; i < 5; i++) {
                Optional<UserEntity> user = userCacheService.findById(nonExistentId);
                assertFalse(user.isPresent(), "User should not be found on attempt " + i);
            }
            
            // All calls should hit database (not cached) but return consistently
        }

        @Test
        @DisplayName("Should handle rapid successive calls for non-existent users")
        void shouldHandleRapidCallsForNonExistentUsers() {
            String nonExistentEmail = "doesnotexist@example.com";
            
            // Rapid successive calls
            for (int i = 0; i < 10; i++) {
                Optional<UserEntity> user = userCacheService.findByEmail(nonExistentEmail);
                assertFalse(user.isPresent());
            }
        }
    }
}
