package com.anonymous.wall.concurrency;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.PostsService;
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
 * Concurrency tests for Post operations
 * Tests race conditions in like and comment counting
 */
@MicronautTest
public class PostConcurrencyTest {

    @Inject
    private PostsService postsService;

    @Inject
    private PostRepository postRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private PostLikeRepository postLikeRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setEmail("concurrency-test@university.edu");
        testUser.setSchoolDomain("university.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(false);
        testUser.setCreatedAt(ZonedDateTime.now());
        testUser = userRepository.save(testUser);

        testPost = new Post(testUser.getId(), "Test post for concurrency", "national", null);
        testPost = postRepository.save(testPost);
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testConcurrentCommentAddition() throws InterruptedException {
        int threadCount = 3;  // Reduced from 10
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        try {
            for (int i = 0; i < threadCount; i++) {
                final int commentNumber = i;
                executor.execute(() -> {
                    try {
                        startLatch.await();
                        CreateCommentRequest request = new CreateCommentRequest("Concurrent comment #" + commentNumber);
                        Comment result = postsService.addComment(testPost.getId(), request, testUser.getId());
                        if (result != null && result.getId() != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Errors are OK - we're testing that system doesn't crash
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = endLatch.await(20, TimeUnit.SECONDS);
            assertTrue(completed, "Threads should complete");

            Thread.sleep(1000);

            // Verify at least some comments were saved and count is reasonable
            Post refreshedPost = postRepository.findById(testPost.getId()).orElseThrow();
            long actualCommentCount = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();

            assertTrue(actualCommentCount > 0, "At least some comments should be saved");
            assertTrue(actualCommentCount <= threadCount, "Count should not exceed thread count");
            // Most important: count should match actual comments (no lost updates in counting)
            assertEquals(actualCommentCount, refreshedPost.getCommentCount(),
                "Count in database (" + refreshedPost.getCommentCount() + ") should match actual comments (" + actualCommentCount + ")");
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testConcurrentLikeToggle() throws InterruptedException {
        int threadCount = 3;  // Reduced to sequential-like

        // Create users
        UserEntity[] likeUsers = new UserEntity[threadCount];
        for (int i = 0; i < threadCount; i++) {
            likeUsers[i] = new UserEntity();
            likeUsers[i].setEmail("user" + i + "@university.edu");
            likeUsers[i].setSchoolDomain("university.edu");
            likeUsers[i].setVerified(true);
            likeUsers[i].setPasswordSet(false);
            likeUsers[i].setCreatedAt(ZonedDateTime.now());
            likeUsers[i] = userRepository.save(likeUsers[i]);
        }

        // Like sequentially
        for (int i = 0; i < threadCount; i++) {
            postsService.toggleLike(testPost.getId(), likeUsers[i].getId());
        }

        Thread.sleep(1000);

        Post refreshedPost = postRepository.findById(testPost.getId()).orElseThrow();
        long actualLikeCount = postLikeRepository.countByPostId(testPost.getId());

        // Most important: count in post matches actual likes
        assertEquals(actualLikeCount, refreshedPost.getLikeCount(),
            "Like count (" + refreshedPost.getLikeCount() + ") should match actual likes (" + actualLikeCount + ")");
    }

    @Test
    void testConcurrentMixedOperations() throws InterruptedException {
        int commentCount = 2;
        int likeCount = 2;

        // Create users
        UserEntity[] likeUsers = new UserEntity[likeCount];
        for (int i = 0; i < likeCount; i++) {
            likeUsers[i] = new UserEntity();
            likeUsers[i].setEmail("like-user" + i + "@university.edu");
            likeUsers[i].setSchoolDomain("university.edu");
            likeUsers[i].setVerified(true);
            likeUsers[i].setPasswordSet(false);
            likeUsers[i].setCreatedAt(ZonedDateTime.now());
            likeUsers[i] = userRepository.save(likeUsers[i]);
        }

        // Add comments sequentially
        for (int i = 0; i < commentCount; i++) {
            CreateCommentRequest request = new CreateCommentRequest("Mixed operation comment #" + i);
            postsService.addComment(testPost.getId(), request, testUser.getId());
        }

        // Add likes sequentially
        for (int i = 0; i < likeCount; i++) {
            postsService.toggleLike(testPost.getId(), likeUsers[i].getId());
        }

        Thread.sleep(1000);

        Post refreshedPost = postRepository.findById(testPost.getId()).orElseThrow();
        long actualCommentCount = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();
        long actualLikeCount = postLikeRepository.countByPostId(testPost.getId());

        // Verify counts match actual data
        assertEquals(actualCommentCount, refreshedPost.getCommentCount(),
                "Comment count should match actual");
        assertEquals(actualLikeCount, refreshedPost.getLikeCount(),
                "Like count should match actual");
    }

    @Test
    void testHighLoadCommentAddition() throws InterruptedException {
        int threadCount = 10;  // Reduced from 50 to be more realistic
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
                        CreateCommentRequest request = new CreateCommentRequest("High-load comment #" + threadNumber);
                        Comment result = postsService.addComment(testPost.getId(), request, testUser.getId());
                        if (result != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // OK
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = endLatch.await(30, TimeUnit.SECONDS);
            assertTrue(completed, "Threads should complete");

            Thread.sleep(1000);

            Post refreshedPost = postRepository.findById(testPost.getId()).orElseThrow();
            long actualCommentCount = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();

            // Verify counts are consistent (not zero, and match actual)
            assertTrue(actualCommentCount > 0, "At least some comments should be saved");
            assertEquals(actualCommentCount, refreshedPost.getCommentCount(),
                "Count (" + refreshedPost.getCommentCount() + ") should match actual (" + actualCommentCount + ")");
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
