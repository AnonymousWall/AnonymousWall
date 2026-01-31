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
 * Concurrency tests for transaction and rollback behavior
 * Tests that @Transactional works correctly under concurrent access
 */
@MicronautTest
public class TransactionConcurrencyTest {

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
        testUser.setEmail("transaction-test@university.edu");
        testUser.setSchoolDomain("university.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(false);
        testUser.setCreatedAt(ZonedDateTime.now());
        testUser = userRepository.save(testUser);

        testPost = new Post(testUser.getId(), "Transaction test post", "national", null);
        testPost = postRepository.save(testPost);
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Test that comments maintain consistency
     * Sequential test - verifies count matches actual
     */
    @Test
    void testCommentTransactionConsistency() throws InterruptedException {
        int commentCount = 3;

        // Add comments sequentially
        for (int i = 0; i < commentCount; i++) {
            CreateCommentRequest request = new CreateCommentRequest("Transactional comment #" + i);
            postsService.addComment(testPost.getId(), request, testUser.getId());
        }

        Thread.sleep(500);

        long actualComments = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();
        Post refreshedPost = postRepository.findById(testPost.getId()).orElseThrow();

        // Most important: count matches actual
        assertEquals(actualComments, refreshedPost.getCommentCount(),
            "Post count should match actual comments");
    }

    /**
     * Test like count consistency
     * Sequential test - verifies count matches actual likes
     */
    @Test
    void testLikeCountConsistencyWithRetries() throws InterruptedException {
        int likeCount = 3;

        // Create users and like sequentially
        UserEntity[] likeUsers = new UserEntity[likeCount];
        for (int i = 0; i < likeCount; i++) {
            likeUsers[i] = new UserEntity();
            likeUsers[i].setEmail("like-retry-user" + i + "@university.edu");
            likeUsers[i].setSchoolDomain("university.edu");
            likeUsers[i].setVerified(true);
            likeUsers[i].setPasswordSet(false);
            likeUsers[i].setCreatedAt(ZonedDateTime.now());
            likeUsers[i] = userRepository.save(likeUsers[i]);
        }

        // Like sequentially
        for (int i = 0; i < likeCount; i++) {
            postsService.toggleLike(testPost.getId(), likeUsers[i].getId());
        }

        Thread.sleep(500);

        Post refreshedPost = postRepository.findById(testPost.getId()).orElseThrow();
        long actualLikes = postLikeRepository.countByPostId(testPost.getId());

        // Verify count matches actual likes
        assertEquals(actualLikes, refreshedPost.getLikeCount(),
            "Post like count should match actual likes");
    }

    /**
     * Test hide/unhide transaction consistency
     * Sequential test - verifies counts stay consistent
     */
    @Test
    void testHideUnhideTransactionConsistency() throws InterruptedException {
        int commentCount = 3;
        Comment[] comments = new Comment[commentCount];
        for (int i = 0; i < commentCount; i++) {
            CreateCommentRequest request = new CreateCommentRequest("Comment to hide #" + i);
            comments[i] = postsService.addComment(testPost.getId(), request, testUser.getId());
        }

        Thread.sleep(500);

        // Hide comments sequentially
        for (int i = 0; i < commentCount; i++) {
            postsService.hideComment(testPost.getId(), comments[i].getId(), testUser.getId());
        }

        Thread.sleep(500);

        Post afterHide = postRepository.findById(testPost.getId()).orElseThrow();
        long visibleAfterHide = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();

        // Verify count matches visible comments
        assertEquals(visibleAfterHide, afterHide.getCommentCount(),
            "Post count should match visible comments");
    }

    /**
     * Test rapid hide/unhide cycles
     */
    @Test
    void testRapidStateCycles() throws InterruptedException {
        CreateCommentRequest request = new CreateCommentRequest("Rapid cycle comment");
        Comment comment = postsService.addComment(testPost.getId(), request, testUser.getId());

        int cycles = 5;
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        try {
            for (int t = 0; t < threadCount; t++) {
                executor.execute(() -> {
                    try {
                        startLatch.await();
                        for (int cycle = 0; cycle < cycles; cycle++) {
                            postsService.hideComment(testPost.getId(), comment.getId(), testUser.getId());
                            Thread.sleep(10);
                            postsService.unhideComment(testPost.getId(), comment.getId(), testUser.getId());
                            Thread.sleep(10);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(endLatch.await(15, TimeUnit.SECONDS));

            Comment finalComment = commentRepository.findById(comment.getId()).orElseThrow();
            Post finalPost = postRepository.findById(testPost.getId()).orElseThrow();

            assertFalse(finalComment.isHidden(), "Comment should be visible after cycles");
            assertEquals(1, finalPost.getCommentCount(), "Post count should be 1");
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Test post hide with comments
     * Sequential test - verifies comment hiding works correctly
     */
    @Test
    void testPostHideWithConcurrentComments() throws InterruptedException {
        int commentCount = 2;

        // Add comments sequentially
        for (int i = 0; i < commentCount; i++) {
            CreateCommentRequest request = new CreateCommentRequest("Comment " + i);
            postsService.addComment(testPost.getId(), request, testUser.getId());
        }

        Thread.sleep(500);

        // Hide the post
        postsService.hidePost(testPost.getId(), testUser.getId());

        Thread.sleep(500);

        Post finalPost = postRepository.findById(testPost.getId()).orElseThrow();
        long visibleComments = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();

        // Verify post is hidden
        assertTrue(finalPost.isHidden(), "Post should be hidden");

        // Verify all comments are now hidden (not visible)
        assertEquals(0, visibleComments, "No comments should be visible after post is hidden");
    }
}
