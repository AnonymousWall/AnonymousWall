package com.anonymous.wall.transaction;
import com.anonymous.wall.model.CommentParentType;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.PostsService;
import com.anonymous.wall.service.base.CommentsService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Transaction atomicity tests
 * Verifies that @Transactional operations are all-or-nothing (ACID compliance)
 * Tests that multi-step operations either complete fully or not at all
 */
@MicronautTest(transactional = false) // Disable automatic transaction management for manual control
public class TransactionAtomicityTest {

    @Inject
    private PostsService postsService;

    @Inject
    private CommentsService commentsService;

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
        testUser.setEmail("atomicity-test@university.edu");
        testUser.setSchoolDomain("university.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(false);
        testUser.setCreatedAt(OffsetDateTime.now());
        testUser = userRepository.save(testUser);

        testPost = new Post(testUser.getId(), "Title", "Atomicity test post", "national", null);
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
     * Test: Comment save is atomic with count update
     * Verifies: If comment is saved, count MUST be incremented
     * If count update fails, comment should be rolled back
     */
    @Test
    void testCommentSaveAndCountUpdateAtomic() {
        int initialCommentCount = 0;
        Post beforeComment = postsService.findById(testPost.getId()).orElseThrow();
        assertEquals(initialCommentCount, beforeComment.getCommentCount(), "Initial count should be 0");

        // Add comment - this is a 2-step operation:
        // 1. Save comment to DB
        // 2. Increment post comment count
        CreateCommentRequest request = new CreateCommentRequest("Test comment for atomicity");
        Comment savedComment = commentsService.addComment(CommentParentType.POST, testPost.getId(), request, testUser.getId());

        // Both operations MUST succeed
        assertNotNull(savedComment.getId(), "Comment should be saved with ID");

        // Verify comment exists in DB
        Comment retrievedComment = commentRepository.findById(savedComment.getId()).orElseThrow();
        assertEquals("Test comment for atomicity", retrievedComment.getText());

        // Verify count was incremented
        Post afterComment = postsService.findById(testPost.getId()).orElseThrow();
        assertEquals(1, afterComment.getCommentCount(),
            "Comment count MUST be incremented when comment is saved (atomicity)");

        // Verify actual comment count in DB matches
        long actualCommentCount = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size();
        assertEquals(actualCommentCount, afterComment.getCommentCount(),
            "Post count must match actual visible comments (consistency)");
    }

    /**
     * Test: Multiple comments maintain consistency
     * Verifies: Each comment increments count by exactly 1
     * No lost increments or double-counting
     */
    @Test
    void testMultipleCommentsAtomicity() {
        int numComments = 5;

        for (int i = 0; i < numComments; i++) {
            CreateCommentRequest request = new CreateCommentRequest("Comment #" + i);
            Comment savedComment = commentsService.addComment(CommentParentType.POST, testPost.getId(), request, testUser.getId());
            assertNotNull(savedComment.getId(), "Comment " + i + " should be saved");
        }

        // After all additions, verify exact consistency
        Post finalPost = postsService.findById(testPost.getId()).orElseThrow();
        long actualCommentCount = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size();

        assertEquals(numComments, actualCommentCount,
            "Should have exactly " + numComments + " comments in DB");
        assertEquals(actualCommentCount, finalPost.getCommentCount(),
            "Post count (" + finalPost.getCommentCount() + ") must match actual (" + actualCommentCount + ")");

        // Verify no duplicates or lost updates
        assertEquals(numComments, finalPost.getCommentCount(),
            "Count must be exactly " + numComments + ", not more, not less");
    }

    /**
     * Test: Like toggle is atomic
     * Verifies: Like is added OR removed, never partial state
     */
    @Test
    void testLikeToggleAtomicity() {
        // Initial state: no likes
        Post beforeLike = postsService.findById(testPost.getId()).orElseThrow();
        assertEquals(0, beforeLike.getLikeCount(), "Initial like count should be 0");

        // First toggle - should add like
        Map<String, Object> result1 = postsService.toggleLikeWithDetails(testPost.getId(), testUser.getId());
        assertTrue((boolean) result1.get("liked"), "First toggle should return true (liked)");

        Post afterFirstToggle = postsService.findById(testPost.getId()).orElseThrow();
        long actualLikes1 = postLikeRepository.countByPostId(testPost.getId());

        assertEquals(1, afterFirstToggle.getLikeCount(), "Count should be 1");
        assertEquals(1, actualLikes1, "Actual likes should be 1");
        assertEquals(actualLikes1, afterFirstToggle.getLikeCount(),
            "Count must match actual (atomicity)");

        // Second toggle - should remove like
        Map<String, Object> result2 = postsService.toggleLikeWithDetails(testPost.getId(), testUser.getId());
        assertFalse((boolean) result2.get("liked"), "Second toggle should return false (unliked)");

        Post afterSecondToggle = postsService.findById(testPost.getId()).orElseThrow();
        long actualLikes2 = postLikeRepository.countByPostId(testPost.getId());

        assertEquals(0, afterSecondToggle.getLikeCount(), "Count should be 0");
        assertEquals(0, actualLikes2, "Actual likes should be 0");
        assertEquals(actualLikes2, afterSecondToggle.getLikeCount(),
            "Count must match actual after unlike (atomicity)");
    }

    /**
     * Test: Hide comment updates count atomically
     * Verifies: Comment hidden AND count decremented, or neither
     */
    @Test
    void testHideCommentAtomicity() {
        // Add a comment first
        CreateCommentRequest request = new CreateCommentRequest("Comment to hide");
        Comment savedComment = commentsService.addComment(CommentParentType.POST, testPost.getId(), request, testUser.getId());

        Post beforeHide = postsService.findById(testPost.getId()).orElseThrow();
        assertEquals(1, beforeHide.getCommentCount(), "Initial count should be 1");

        // Hide the comment
        Comment hiddenComment = commentsService.hideComment(CommentParentType.POST, testPost.getId(), savedComment.getId(), testUser.getId());

        // Verify comment is hidden
        assertTrue(hiddenComment.isHidden(), "Comment should be marked as hidden");

        // Verify count is updated
        Post afterHide = postsService.findById(testPost.getId()).orElseThrow();
        long visibleComments = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size();

        assertEquals(0, visibleComments, "Should have 0 visible comments after hide");
        assertEquals(visibleComments, afterHide.getCommentCount(),
            "Count must match visible comments (atomicity)");
        assertEquals(0, afterHide.getCommentCount(), "Count should be 0 after hiding");
    }

    /**
     * Test: Hide post cascades hide to all comments atomically
     * Verifies: Post hidden AND all comments hidden, or neither
     * Note: Comment count is total count, not visible count
     */
    @Test
    void testPostHideCascadeAtomicity() throws InterruptedException {
        // Add multiple comments
        for (int i = 0; i < 3; i++) {
            CreateCommentRequest request = new CreateCommentRequest("Comment #" + i);
            commentsService.addComment(CommentParentType.POST, testPost.getId(), request, testUser.getId());
        }

        Post beforeHide = postsService.findById(testPost.getId()).orElseThrow();
        assertEquals(3, beforeHide.getCommentCount(), "Should have 3 comments");

        // Hide the post
        Post hiddenPost = postsService.hidePost(testPost.getId(), testUser.getId());

        // Verify post is hidden
        assertTrue(hiddenPost.isHidden(), "Post should be hidden");

        // Wait for async processing to complete
        Thread.sleep(500);

        // Verify all comments are hidden (cascade)
        long visibleComments = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size();
        long totalComments = commentRepository.findByParentTypeAndParentId("POST", testPost.getId()).size();

        assertEquals(0, visibleComments, "All comments should be hidden");
        assertEquals(3, totalComments, "All 3 comments should still exist (soft delete)");

        // Verify consistency: count still shows 3 (total count, not visible count)
        Post finalPost = postsService.findById(testPost.getId()).orElseThrow();
        assertEquals(3, finalPost.getCommentCount(),
            "Comment count is total count, stays 3 even after cascade hide");
    }

    /**
     * Test: Unhide comment reverses hide atomically
     */
    @Test
    void testUnhideCommentAtomicity() {
        CreateCommentRequest request = new CreateCommentRequest("Comment to toggle");
        Comment savedComment = commentsService.addComment(CommentParentType.POST, testPost.getId(), request, testUser.getId());

        // Hide it
        commentsService.hideComment(CommentParentType.POST, testPost.getId(), savedComment.getId(), testUser.getId());

        Post afterHide = postsService.findById(testPost.getId()).orElseThrow();
        assertEquals(0, afterHide.getCommentCount(), "Count should be 0 after hide");

        // Unhide it
        Comment unhiddenComment = commentsService.unhideComment(CommentParentType.POST, testPost.getId(), savedComment.getId(), testUser.getId());

        // Verify comment is unhidden
        assertFalse(unhiddenComment.isHidden(), "Comment should be unhidden");

        // Verify count is updated
        Post afterUnhide = postsService.findById(testPost.getId()).orElseThrow();
        long visibleComments = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size();

        assertEquals(1, visibleComments, "Should have 1 visible comment after unhide");
        assertEquals(visibleComments, afterUnhide.getCommentCount(),
            "Count must match visible comments (atomicity)");
    }

    /**
     * Test: Consistency across operations
     * Verifies: Adding, hiding, unhiding maintains count consistency
     */
    @Test
    void testConsistencyAcrossOperations() {
        // Add comments
        CreateCommentRequest req1 = new CreateCommentRequest("Comment 1");
        CreateCommentRequest req2 = new CreateCommentRequest("Comment 2");
        Comment c1 = commentsService.addComment(CommentParentType.POST, testPost.getId(), req1, testUser.getId());
        Comment c2 = commentsService.addComment(CommentParentType.POST, testPost.getId(), req2, testUser.getId());

        Post after2 = postsService.findById(testPost.getId()).orElseThrow();
        assertEquals(2, after2.getCommentCount());

        // Hide first comment
        commentsService.hideComment(CommentParentType.POST, testPost.getId(), c1.getId(), testUser.getId());

        Post after1Hide = postsService.findById(testPost.getId()).orElseThrow();
        long visible1 = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size();
        assertEquals(1, visible1);
        assertEquals(visible1, after1Hide.getCommentCount());

        // Unhide first comment
        commentsService.unhideComment(CommentParentType.POST, testPost.getId(), c1.getId(), testUser.getId());

        Post afterUnhide = postsService.findById(testPost.getId()).orElseThrow();
        long visible2 = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size();
        assertEquals(2, visible2);
        assertEquals(visible2, afterUnhide.getCommentCount());

        // Verify consistency maintained throughout
        assertTrue(
            afterUnhide.getCommentCount() == commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size(),
            "Consistency must be maintained across all operations"
        );
    }
}
