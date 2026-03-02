package com.anonymous.wall.transaction;
import com.anonymous.wall.model.CommentParentType;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.PostsService;
import com.anonymous.wall.service.CommentsService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Transaction rollback and error handling tests
 * Verifies that @Transactional properly rolls back on errors
 * Tests that failed operations don't leave database in invalid state
 */
@MicronautTest(transactional = false)
public class TransactionRollbackTest {

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
    private UserEntity otherUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setEmail("rollback-test@university.edu");
        testUser.setSchoolDomain("university.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(false);
        testUser.setCreatedAt(OffsetDateTime.now());
        testUser = userRepository.save(testUser);

        otherUser = new UserEntity();
        otherUser.setEmail("other-user@university.edu");
        otherUser.setSchoolDomain("university.edu");
        otherUser.setVerified(true);
        otherUser.setPasswordSet(false);
        otherUser.setCreatedAt(OffsetDateTime.now());
        otherUser = userRepository.save(otherUser);

        testPost = new Post(testUser.getId(), "Title", "Rollback test post", "national", null);
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
     * Test: Failed hide operation on non-existent comment
     * Verifies: Exception doesn't corrupt state
     */
    @Test
    void testHideNonExistentCommentError() {
        CreateCommentRequest req = new CreateCommentRequest("Valid comment");
        commentsService.addComment(CommentParentType.POST, testPost.getId(), req, testUser.getId());

        Post beforeError = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(1, beforeError.getCommentCount());

        // Try to hide non-existent comment - should throw error
        UUID fakeCommentId = UUID.randomUUID();
        assertThrows(Exception.class, () -> {
            commentsService.hideComment(CommentParentType.POST, testPost.getId(), fakeCommentId, testUser.getId());
        }, "Should throw exception for non-existent comment");

        // Verify state wasn't corrupted
        Post afterError = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(1, afterError.getCommentCount(),
            "Count should be unchanged after failed operation");
        long actualComments = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size();
        assertEquals(1, actualComments);
    }

    /**
     * Test: Failed hide operation - permission denied
     * Verifies: Count stays consistent when operation is rejected
     */
    @Test
    void testHideCommentPermissionError() {
        CreateCommentRequest req = new CreateCommentRequest("Comment by original user");
        var comment = commentsService.addComment(CommentParentType.POST, testPost.getId(), req, testUser.getId());

        Post beforeAttempt = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(1, beforeAttempt.getCommentCount());

        // Other user tries to hide testUser's comment - should fail
        assertThrows(Exception.class, () -> {
            commentsService.hideComment(CommentParentType.POST, testPost.getId(), comment.getId(), otherUser.getId());
        }, "Should throw exception for permission denied");

        // Verify state unchanged
        Post afterAttempt = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(1, afterAttempt.getCommentCount(),
            "Count should be unchanged after failed hide");
        long visibleComments = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size();
        assertEquals(1, visibleComments);
    }

    /**
     * Test: Partial operation failure doesn't corrupt consistency
     * Verifies: Transaction atomicity prevents partial updates
     */
    @Test
    void testPartialOperationFailure() {
        // Add a comment
        CreateCommentRequest req1 = new CreateCommentRequest("Comment 1");
        var c1 = commentsService.addComment(CommentParentType.POST, testPost.getId(), req1, testUser.getId());

        Post after1 = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(1, after1.getCommentCount());

        // Try to hide a non-existent comment on the same post
        // If transaction is atomic, no partial update should occur
        UUID fakeId = UUID.randomUUID();
        try {
            commentsService.hideComment(CommentParentType.POST, testPost.getId(), fakeId, testUser.getId());
        } catch (Exception e) {
            // Expected to fail
        }

        // Verify no partial state
        Post afterFailure = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(1, afterFailure.getCommentCount(),
            "Count should still be 1 - no partial updates");

        var comments = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId());
        assertEquals(1, comments.size());
        assertFalse(comments.get(0).isHidden());
    }

    /**
     * Test: Like operation doesn't leave partial state
     * Verifies: Like add/remove is fully committed or fully rolled back
     */
    @Test
    void testLikeOperationAtomicity() {
        Post before = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(0, before.getLikeCount());

        // Add like
        Map<String, Object> liked = postsService.toggleLikeWithDetails(testPost.getId(), testUser.getId());
        assertTrue((boolean) liked.get("liked"));

        Post afterLike = postRepository.findById(testPost.getId()).orElseThrow();
        long likeCount = postLikeRepository.countByPostId(testPost.getId());

        assertEquals(1, afterLike.getLikeCount());
        assertEquals(1, likeCount);
        assertEquals(afterLike.getLikeCount(), likeCount, "Must be consistent");

        // Remove like
        Map<String, Object> unliked = postsService.toggleLikeWithDetails(testPost.getId(), testUser.getId());
        assertFalse((boolean) unliked.get("liked"));

        Post afterUnlike = postRepository.findById(testPost.getId()).orElseThrow();
        long likeCountAfter = postLikeRepository.countByPostId(testPost.getId());

        assertEquals(0, afterUnlike.getLikeCount());
        assertEquals(0, likeCountAfter);
        assertEquals(afterUnlike.getLikeCount(), likeCountAfter, "Must be consistent");
    }

    /**
     * Test: Post hide cascade doesn't partially hide comments
     * Verifies: Either all comments hidden or transaction rolls back
     */
    @Test
    void testPostHideCascadeAtomicity() throws InterruptedException {
        // Add multiple comments
        for (int i = 0; i < 3; i++) {
            CreateCommentRequest req = new CreateCommentRequest("Comment " + i);
            commentsService.addComment(CommentParentType.POST, testPost.getId(), req, testUser.getId());
        }

        Post before = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(3, before.getCommentCount());

        // Hide post - should cascade hide all comments
        postsService.hidePost(testPost.getId(), testUser.getId());

        // Wait for async processing to complete
        Thread.sleep(500);

        Post after = postRepository.findById(testPost.getId()).orElseThrow();
        long visibleComments = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size();
        long totalComments = commentRepository.findByParentTypeAndParentId("POST", testPost.getId()).size();

        // Either ALL comments are hidden or NONE are hidden (atomicity)
        assertTrue(visibleComments == 0 || visibleComments == 3,
            "Cascade hide must be atomic - all or nothing");

        // In this case, all should be hidden
        assertEquals(0, visibleComments);
        assertEquals(3, totalComments, "Total comments should still exist (soft delete)");
    }

    /**
     * Test: Consistency after operation sequence with errors
     * Verifies: State remains consistent even after mixed success/failure
     */
    @Test
    void testConsistencyAfterMixedOperations() {
        // Success: add comment
        CreateCommentRequest req1 = new CreateCommentRequest("Comment 1");
        var c1 = commentsService.addComment(CommentParentType.POST, testPost.getId(), req1, testUser.getId());
        Post s1 = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(1, s1.getCommentCount());

        // Success: add another comment
        CreateCommentRequest req2 = new CreateCommentRequest("Comment 2");
        var c2 = commentsService.addComment(CommentParentType.POST, testPost.getId(), req2, testUser.getId());
        Post s2 = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(2, s2.getCommentCount());

        // Failure: try to hide non-existent comment
        try {
            commentsService.hideComment(CommentParentType.POST, testPost.getId(), UUID.randomUUID(), testUser.getId());
        } catch (Exception e) {
            // Expected
        }
        Post s3 = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(2, s3.getCommentCount(), "Count should be unchanged after failure");

        // Success: like post
        postsService.toggleLikeWithDetails(testPost.getId(), testUser.getId());
        Post s4 = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(2, s4.getCommentCount());
        assertEquals(1, s4.getLikeCount());

        // Verify consistency throughout
        long visibleComments = commentRepository.findByParentTypeAndParentIdAndHiddenFalse("POST", testPost.getId()).size();
        assertEquals(visibleComments, s4.getCommentCount(), "Consistency maintained");
    }

    /**
     * Test: No count corruption from repeated failed operations
     * Verifies: Failed operations don't accumulate errors
     */
    @Test
    void testRepeatedFailedOperations() {
        CreateCommentRequest req = new CreateCommentRequest("Real comment");
        commentsService.addComment(CommentParentType.POST, testPost.getId(), req, testUser.getId());

        Post initial = postRepository.findById(testPost.getId()).orElseThrow();
        int initialCount = initial.getCommentCount();

        // Try to hide non-existent comment multiple times
        for (int i = 0; i < 5; i++) {
            try {
                commentsService.hideComment(CommentParentType.POST, testPost.getId(), UUID.randomUUID(), testUser.getId());
            } catch (Exception e) {
                // Expected
            }
        }

        Post final_post = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(initialCount, final_post.getCommentCount(),
            "Count should be unchanged after repeated failures");
    }
}
