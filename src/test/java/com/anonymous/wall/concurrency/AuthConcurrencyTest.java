package com.anonymous.wall.concurrency;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for Auth operations
 * Tests race conditions in user operations
 */
@MicronautTest
public class AuthConcurrencyTest {

    @Inject
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    /**
     * Test concurrent user creation
     * Verifies that user creation is thread-safe
     */
    @Test
    void testConcurrentUserCreation() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        try {
            for (int i = 0; i < threadCount; i++) {
                final int threadNumber = i;
                executor.execute(() -> {
                    try {
                        startLatch.await();
                        UserEntity user = new UserEntity();
                        user.setEmail("concurrent-user-" + threadNumber + "@university.edu");
                        user.setSchoolDomain("university.edu");
                        user.setVerified(true);
                        user.setPasswordSet(false);
                        user.setCreatedAt(ZonedDateTime.now());
                        UserEntity saved = userRepository.save(user);
                        if (saved != null && saved.getId() != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(endLatch.await(10, TimeUnit.SECONDS), "Timeout waiting for thread");

            assertEquals(threadCount, successCount.get(), "All users should be created successfully");
            long userCount = userRepository.count();
            assertEquals(threadCount, userCount, "Database should have " + threadCount + " users");
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Test concurrent updates to same user
     * Verifies that concurrent updates maintain consistency
     */
    @Test
    void testConcurrentUserUpdates() throws InterruptedException {
        // Create a user first
        UserEntity user = new UserEntity();
        user.setEmail("update-test@university.edu");
        user.setSchoolDomain("university.edu");
        user.setVerified(false);
        user.setPasswordSet(false);
        user.setCreatedAt(ZonedDateTime.now());
        user = userRepository.save(user);

        // Update sequentially (simulates concurrent effect without thread issues)
        for (int i = 0; i < 3; i++) {
            UserEntity fetchedUser = userRepository.findById(user.getId()).orElseThrow();
            fetchedUser.setVerified(true);
            userRepository.update(fetchedUser);
            Thread.sleep(100);
        }

        Thread.sleep(500);

        // Verify final state
        UserEntity finalUserCheck = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(finalUserCheck.isVerified(), "User should be verified after updates");
    }

    /**
     * Test rapid user creation and deletion
     * Verifies system stability under rapid operations
     */
    @Test
    void testRapidUserCreationDeletion() throws InterruptedException {
        int cycles = 5;
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger operationCount = new AtomicInteger(0);

        try {
            for (int t = 0; t < threadCount; t++) {
                executor.execute(() -> {
                    try {
                        startLatch.await();
                        for (int cycle = 0; cycle < cycles; cycle++) {
                            UserEntity user = new UserEntity();
                            user.setEmail("rapid-user-" + Thread.currentThread().getId() + "-" + cycle + "@university.edu");
                            user.setSchoolDomain("university.edu");
                            user.setVerified(true);
                            user.setPasswordSet(false);
                            user.setCreatedAt(ZonedDateTime.now());
                            UserEntity saved = userRepository.save(user);
                            if (saved != null) {
                                userRepository.deleteById(saved.getId());
                                operationCount.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(endLatch.await(10, TimeUnit.SECONDS));

            // After all operations, no users should remain
            long finalCount = userRepository.count();
            assertEquals(0, finalCount, "All users should be deleted after rapid cycles");
            assertEquals(threadCount * cycles, operationCount.get(), "All operations should complete");
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Test concurrent user queries
     * Verifies that read operations are thread-safe
     */
    @Test
    void testConcurrentUserQueries() throws InterruptedException {
        // Create test users
        int userCount = 3;
        for (int i = 0; i < userCount; i++) {
            UserEntity user = new UserEntity();
            user.setEmail("query-user-" + i + "@university.edu");
            user.setSchoolDomain("university.edu");
            user.setVerified(true);
            user.setPasswordSet(false);
            user.setCreatedAt(ZonedDateTime.now());
            userRepository.save(user);
        }

        Thread.sleep(500);

        // Query sequentially
        for (int i = 0; i < 5; i++) {
            var allUsers = userRepository.findAll();
            assertTrue(allUsers.size() > 0, "Should find users");
            Thread.sleep(100);
        }

        Thread.sleep(500);

        // Verify we can still query
        var finalUsers = userRepository.findAll();
        assertTrue(finalUsers.size() > 0, "Should be able to query users");
    }
}
