package com.anonymous.wall.concurrency;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for Auth / User repository operations.
 *
 * IMPORTANT: @MicronautTest is used with transactional = false.
 * Without this, every test method runs inside a single transaction that is
 * never committed until the test ends. Worker threads in a separate thread
 * pool have no access to that uncommitted transaction, so they always see
 * an empty database. Setting transactional = false forces each repository
 * call to commit immediately and become visible to all threads.
 *
 * Cleanup is handled manually in @BeforeEach / @AfterEach.
 */
@MicronautTest(transactional = false)
@DisplayName("Auth Concurrency Tests")
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

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private UserEntity buildUser(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setSchoolDomain("university.edu");
        user.setVerified(true);
        user.setPasswordSet(false);
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 10 threads each create a distinct user simultaneously.
     * All inserts must succeed and exactly 10 rows must exist afterwards.
     */
    @Test
    @DisplayName("Concurrent user creation — all distinct emails should succeed")
    void testConcurrentUserCreation() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        try {
            for (int i = 0; i < threadCount; i++) {
                final int threadNumber = i;
                executor.execute(() -> {
                    try {
                        startLatch.await();
                        UserEntity saved = userRepository.save(
                                buildUser("concurrent-user-" + threadNumber + "@university.edu"));
                        if (saved != null && saved.getId() != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(endLatch.await(15, TimeUnit.SECONDS), "Timed out waiting for threads");

            assertTrue(errors.isEmpty(), "Unexpected errors in worker threads: " + errors);
            assertEquals(threadCount, successCount.get(), "All inserts should succeed");
            assertEquals(threadCount, userRepository.count(),
                    "DB should contain exactly " + threadCount + " users");
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
                    "Executor did not terminate cleanly");
        }
    }

    /**
     * Two threads race to insert the same email address.
     * Exactly one must succeed; the other must receive a constraint-violation
     * exception, leaving only 1 row in the DB.
     */
    @Test
    @DisplayName("Concurrent duplicate email insert — exactly one insert must win")
    void testConcurrentDuplicateEmailInsert() throws InterruptedException {
        String duplicateEmail = "duplicate-" + UUID.randomUUID() + "@university.edu";
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        try {
            for (int i = 0; i < threadCount; i++) {
                executor.execute(() -> {
                    try {
                        startLatch.await();
                        userRepository.save(buildUser(duplicateEmail));
                        successCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        // Expected: one thread will hit a unique-constraint violation
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(endLatch.await(15, TimeUnit.SECONDS), "Timed out waiting for threads");

            assertEquals(1, successCount.get(), "Only one insert should win the duplicate race");
            assertEquals(1, userRepository.count(), "DB should contain exactly 1 user");
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
                    "Executor did not terminate cleanly");
        }
    }

    /**
     * 5 threads concurrently update the same user.
     * The entity must remain readable and structurally valid after all updates.
     * At least one update must succeed; others may fail with optimistic-lock
     * conflicts, which is acceptable behaviour.
     */
    @Test
    @DisplayName("Concurrent updates to the same user — final state must be consistent")
    void testConcurrentUserUpdates() throws InterruptedException {
        // Committed immediately because transactional = false
        UserEntity saved = userRepository.save(buildUser("update-test@university.edu"));
        final UUID userId = saved.getId();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        try {
            for (int i = 0; i < threadCount; i++) {
                final boolean verifiedValue = (i % 2 == 0);
                executor.execute(() -> {
                    try {
                        startLatch.await();
                        UserEntity fetched = userRepository.findById(userId).orElseThrow();
                        fetched.setVerified(verifiedValue);
                        userRepository.update(fetched);
                        successCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        // Optimistic-lock conflicts are acceptable; log but don't fail
                        errors.add(e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(endLatch.await(15, TimeUnit.SECONDS), "Timed out waiting for threads");

            // Entity must still exist and be structurally valid after concurrent updates
            UserEntity finalState = userRepository.findById(userId).orElseThrow(
                    () -> new AssertionError("User was deleted during concurrent update"));
            assertNotNull(finalState.getId(), "User ID must not be null after concurrent updates");
            assertEquals("update-test@university.edu", finalState.getEmail(),
                    "Email must be unchanged after concurrent updates");
            // At least one thread must have succeeded
            assertTrue(successCount.get() >= 1,
                    "At least one concurrent update must succeed, but got: " + successCount.get()
                            + ". Errors: " + errors);
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
                    "Executor did not terminate cleanly");
        }
    }

    /**
     * 4 threads each create and immediately delete their own users across 5 cycles.
     * After all threads finish, no rows should remain.
     */
    @Test
    @DisplayName("Rapid concurrent create+delete cycles — no rows should remain")
    void testRapidUserCreationDeletion() throws InterruptedException {
        int cycles = 5;
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger operationCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        try {
            for (int t = 0; t < threadCount; t++) {
                executor.execute(() -> {
                    try {
                        startLatch.await();
                        for (int cycle = 0; cycle < cycles; cycle++) {
                            String email = "rapid-" + UUID.randomUUID() + "@university.edu";
                            UserEntity user = userRepository.save(buildUser(email));
                            userRepository.deleteById(user.getId());
                            operationCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(endLatch.await(15, TimeUnit.SECONDS), "Timed out waiting for threads");

            assertTrue(errors.isEmpty(), "Unexpected errors in worker threads: " + errors);
            assertEquals(0, userRepository.count(),
                    "All users should be deleted after rapid cycles");
            assertEquals(threadCount * cycles, operationCount.get(),
                    "All create+delete cycles should complete");
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
                    "Executor did not terminate cleanly");
        }
    }

    /**
     * 5 threads concurrently query the user table while 3 known users exist.
     * Every thread must observe at least the 3 pre-seeded users.
     * Pre-seeded rows are committed before threads start (transactional = false).
     */
    @Test
    @DisplayName("Concurrent reads — all threads must observe the pre-seeded users")
    void testConcurrentUserQueries() throws InterruptedException {
        int seededCount = 3;
        // These saves commit immediately because transactional = false
        for (int i = 0; i < seededCount; i++) {
            userRepository.save(buildUser("query-user-" + i + "@university.edu"));
        }

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        try {
            for (int t = 0; t < threadCount; t++) {
                executor.execute(() -> {
                    try {
                        startLatch.await();
                        long count = userRepository.count();
                        if (count >= seededCount) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(endLatch.await(15, TimeUnit.SECONDS), "Timed out waiting for threads");

            assertTrue(errors.isEmpty(), "Unexpected errors in reader threads: " + errors);
            assertEquals(threadCount, successCount.get(),
                    "Every reader thread must observe at least the " + seededCount + " seeded users");
            assertEquals(seededCount, userRepository.count(),
                    "Seeded user count must be unchanged after reads");
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
                    "Executor did not terminate cleanly");
        }
    }
}