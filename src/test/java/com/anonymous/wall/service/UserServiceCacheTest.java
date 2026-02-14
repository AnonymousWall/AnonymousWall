package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.cache.CacheManager;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MicronautTest
@DisplayName("UserService Cache Tests")
class UserServiceCacheTest {

    @Inject
    UserService userService;

    @Inject
    UserRepository userRepository;

    @Inject
    CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        cacheManager.getCache("blocked-users").ifPresent(cache -> cache.invalidateAll());
        Mockito.reset(userRepository);
    }

    @Test
    @DisplayName("Should cache blocked user status and avoid repeated DB calls")
    void shouldCacheBlockedStatus() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserEntity blockedUser = new UserEntity();
        blockedUser.setId(userId);
        blockedUser.setBlocked(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(blockedUser));

        // Act - First call should hit database
        boolean isBlocked1 = userService.isUserBlocked(userId);
        
        // Act - Second call should use cache
        boolean isBlocked2 = userService.isUserBlocked(userId);
        
        // Act - Third call should also use cache
        boolean isBlocked3 = userService.isUserBlocked(userId);

        // Assert
        assertTrue(isBlocked1, "First call should return true");
        assertTrue(isBlocked2, "Second call should return true from cache");
        assertTrue(isBlocked3, "Third call should return true from cache");
        
        // Verify repository was called only once (cache hit on subsequent calls)
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should cache non-blocked user status")
    void shouldCacheNonBlockedStatus() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserEntity nonBlockedUser = new UserEntity();
        nonBlockedUser.setId(userId);
        nonBlockedUser.setBlocked(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(nonBlockedUser));

        // Act
        boolean isBlocked1 = userService.isUserBlocked(userId);
        boolean isBlocked2 = userService.isUserBlocked(userId);
        boolean isBlocked3 = userService.isUserBlocked(userId);

        // Assert
        assertFalse(isBlocked1, "First call should return false");
        assertFalse(isBlocked2, "Second call should return false from cache");
        assertFalse(isBlocked3, "Third call should return false from cache");
        
        // Verify repository was called only once
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should handle user not found gracefully and cache the result")
    void shouldCacheUserNotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        boolean isBlocked1 = userService.isUserBlocked(userId);
        boolean isBlocked2 = userService.isUserBlocked(userId);

        // Assert
        assertFalse(isBlocked1, "Should return false when user not found");
        assertFalse(isBlocked2, "Should return false from cache when user not found");
        
        // Verify repository was called only once
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should invalidate cache when user is updated")
    void shouldInvalidateCacheOnUpdate() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setBlocked(false);

        UserEntity updatedUser = new UserEntity();
        updatedUser.setId(userId);
        updatedUser.setBlocked(true);

        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user))
            .thenReturn(Optional.of(updatedUser));
        when(userRepository.update(updatedUser)).thenReturn(updatedUser);

        // Act - First call should cache non-blocked status
        boolean isBlocked1 = userService.isUserBlocked(userId);
        
        // Update user (should invalidate cache)
        userService.update(updatedUser);
        
        // Second call should fetch fresh data from DB
        boolean isBlocked2 = userService.isUserBlocked(userId);

        // Assert
        assertFalse(isBlocked1, "Before update: user should not be blocked");
        assertTrue(isBlocked2, "After update: user should be blocked with fresh data from DB");
        
        // Verify repository was called twice (once before update, once after cache invalidation)
        verify(userRepository, times(2)).findById(userId);
    }

    @Test
    @DisplayName("Should be thread-safe under concurrent access")
    void shouldBeThreadSafe() throws InterruptedException {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setBlocked(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        int threadCount = 10;
        int callsPerThread = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger trueCount = new AtomicInteger(0);

        // Act - Multiple threads accessing cache concurrently
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < callsPerThread; j++) {
                        if (userService.isUserBlocked(userId)) {
                            trueCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Assert
        assertEquals(threadCount * callsPerThread, trueCount.get(), 
            "All calls should return true consistently");
        
        // Repository should be called a minimal number of times (cache should work)
        // Due to concurrent access, it might be called a few times, but not 50 times
        verify(userRepository, atMost(5)).findById(userId);
    }

    @Test
    @DisplayName("Should cache status for different users independently")
    void shouldCacheMultipleUsersIndependently() {
        // Arrange
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        
        UserEntity blockedUser = new UserEntity();
        blockedUser.setId(userId1);
        blockedUser.setBlocked(true);
        
        UserEntity nonBlockedUser = new UserEntity();
        nonBlockedUser.setId(userId2);
        nonBlockedUser.setBlocked(false);

        when(userRepository.findById(userId1)).thenReturn(Optional.of(blockedUser));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(nonBlockedUser));

        // Act
        boolean user1Blocked1 = userService.isUserBlocked(userId1);
        boolean user2Blocked1 = userService.isUserBlocked(userId2);
        boolean user1Blocked2 = userService.isUserBlocked(userId1);
        boolean user2Blocked2 = userService.isUserBlocked(userId2);

        // Assert
        assertTrue(user1Blocked1, "User 1 should be blocked");
        assertFalse(user2Blocked1, "User 2 should not be blocked");
        assertTrue(user1Blocked2, "User 1 should still be blocked (from cache)");
        assertFalse(user2Blocked2, "User 2 should still not be blocked (from cache)");
        
        // Each user's repository call should happen only once
        verify(userRepository, times(1)).findById(userId1);
        verify(userRepository, times(1)).findById(userId2);
    }
}
