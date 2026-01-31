package com.anonymous.wall.transaction;

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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Transaction consistency tests
 * Verifies that @Transactional operations maintain consistency
 * Tests that database state is always valid, never corrupted
 */
@MicronautTest
public class TransactionConsistencyTest {

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
        testUser.setEmail("consistency-test@university.edu");
        testUser.setSchoolDomain("university.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(false);
        testUser.setCreatedAt(ZonedDateTime.now());
        testUser = userRepository.save(testUser);

        testPost = new Post(testUser.getId(), "Consistency test post", "national", null);
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
     * Test: Comment count never exceeds actual comments
     * Verifies: Count is never inflated due to failed operations
     */
    @Test
    void testCommentCountNeverInflated() {
        CreateCommentRequest req = new CreateCommentRequest("Comment 1");
        postsService.addComment(testPost.getId(), req, testUser.getId());

        Post post1 = postRepository.findById(testPost.getId()).orElseThrow();
        long actual1 = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();

        assertEquals(1, post1.getCommentCount());
        assertEquals(1, actual1);
        assertTrue(post1.getCommentCount() <= actual1, "Count should never exceed actual");

        // Add another
        CreateCommentRequest req2 = new CreateCommentRequest("Comment 2");
        postsService.addComment(testPost.getId(), req2, testUser.getId());

        Post post2 = postRepository.findById(testPost.getId()).orElseThrow();
        long actual2 = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();

        assertEquals(2, post2.getCommentCount());
        assertEquals(2, actual2);
        assertTrue(post2.getCommentCount() <= actual2, "Count should never exceed actual");
    }

    /**
     * Test: Like count never exceeds actual likes
     * Verifies: No duplicate counting of likes
     */
    @Test
    void testLikeCountAccurate() {
        // Add like
        postsService.toggleLike(testPost.getId(), testUser.getId());

        Post post1 = postRepository.findById(testPost.getId()).orElseThrow();
        long actual1 = postLikeRepository.countByPostId(testPost.getId());

        assertEquals(1, post1.getLikeCount());
        assertEquals(1, actual1);
        assertEquals(post1.getLikeCount(), actual1, "Counts must match exactly");

        // Remove like
        postsService.toggleLike(testPost.getId(), testUser.getId());

        Post post2 = postRepository.findById(testPost.getId()).orElseThrow();
        long actual2 = postLikeRepository.countByPostId(testPost.getId());

        assertEquals(0, post2.getLikeCount());
        assertEquals(0, actual2);
        assertEquals(post2.getLikeCount(), actual2, "Counts must match exactly");
    }

    /**
     * Test: Visible comment count accurately reflects hidden state
     * Verifies: Hiding/unhiding updates count consistently
     */
    @Test
    void testVisibleCommentCountConsistency() {
        // Add 3 comments
        Comment[] comments = new Comment[3];
        for (int i = 0; i < 3; i++) {
            CreateCommentRequest req = new CreateCommentRequest("Comment " + i);
            comments[i] = postsService.addComment(testPost.getId(), req, testUser.getId());
        }

        Post post1 = postRepository.findById(testPost.getId()).orElseThrow();
        long visible1 = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();
        assertEquals(3, post1.getCommentCount());
        assertEquals(3, visible1);

        // Hide one
        postsService.hideComment(testPost.getId(), comments[0].getId(), testUser.getId());

        Post post2 = postRepository.findById(testPost.getId()).orElseThrow();
        long visible2 = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();
        assertEquals(2, visible2);
        assertEquals(visible2, post2.getCommentCount(), "Count must match visible");

        // Hide another
        postsService.hideComment(testPost.getId(), comments[1].getId(), testUser.getId());

        Post post3 = postRepository.findById(testPost.getId()).orElseThrow();
        long visible3 = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();
        assertEquals(1, visible3);
        assertEquals(visible3, post3.getCommentCount(), "Count must match visible");

        // Unhide one
        postsService.unhideComment(testPost.getId(), comments[0].getId(), testUser.getId());

        Post post4 = postRepository.findById(testPost.getId()).orElseThrow();
        long visible4 = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();
        assertEquals(2, visible4);
        assertEquals(visible4, post4.getCommentCount(), "Count must match visible");
    }

    /**
     * Test: Database referential integrity
     * Verifies: No orphaned comments (all comments have valid post)
     */
    @Test
    void testReferentialIntegrity() {
        CreateCommentRequest req = new CreateCommentRequest("Comment for integrity check");
        Comment comment = postsService.addComment(testPost.getId(), req, testUser.getId());

        // Verify comment has valid post
        assertTrue(comment.getPostId() > 0, "Comment should reference a post");
        assertEquals(testPost.getId(), comment.getPostId(), "Comment should reference correct post");

        // Verify post exists
        Post post = postRepository.findById(comment.getPostId()).orElseThrow();
        assertNotNull(post, "Referenced post should exist");

        // Verify post has this comment
        long commentCount = commentRepository.countByPostId(post.getId());
        assertTrue(commentCount > 0, "Post should have at least one comment");
    }

    /**
     * Test: No lost updates on multiple quick operations
     * Verifies: Each operation is counted correctly
     */
    @Test
    void testNoLostUpdates() {
        int operationCount = 10;

        for (int i = 0; i < operationCount; i++) {
            CreateCommentRequest req = new CreateCommentRequest("Quick comment #" + i);
            postsService.addComment(testPost.getId(), req, testUser.getId());
        }

        Post finalPost = postRepository.findById(testPost.getId()).orElseThrow();
        long actualCount = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();

        assertEquals(operationCount, actualCount,
            "Should have exactly " + operationCount + " comments");
        assertEquals(actualCount, finalPost.getCommentCount(),
            "Count must match actual - no lost updates");
        assertEquals(operationCount, finalPost.getCommentCount(),
            "Count must be exactly " + operationCount);
    }

    /**
     * Test: Consistency after complex sequence
     * Verifies: Multiple operation types maintain consistency
     */
    @Test
    void testComplexOperationSequence() {
        // Add comments
        Comment c1 = postsService.addComment(testPost.getId(), new CreateCommentRequest("Comment 1"), testUser.getId());
        Comment c2 = postsService.addComment(testPost.getId(), new CreateCommentRequest("Comment 2"), testUser.getId());
        Comment c3 = postsService.addComment(testPost.getId(), new CreateCommentRequest("Comment 3"), testUser.getId());

        Post s1 = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(3, s1.getCommentCount());

        // Hide one comment
        postsService.hideComment(testPost.getId(), c1.getId(), testUser.getId());
        Post s2 = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(2, s2.getCommentCount());

        // Add like
        postsService.toggleLike(testPost.getId(), testUser.getId());
        Post s3 = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(2, s3.getCommentCount());
        assertEquals(1, s3.getLikeCount());

        // Unhide comment
        postsService.unhideComment(testPost.getId(), c1.getId(), testUser.getId());
        Post s4 = postRepository.findById(testPost.getId()).orElseThrow();
        assertEquals(3, s4.getCommentCount());
        assertEquals(1, s4.getLikeCount());

        // Hide post (cascades)
        postsService.hidePost(testPost.getId(), testUser.getId());
        Post s5 = postRepository.findById(testPost.getId()).orElseThrow();
        assertTrue(s5.isHidden());
        long visibleComments = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();
        assertEquals(0, visibleComments, "No visible comments after post hide");
        assertEquals(3, s5.getCommentCount(), "Total count stays 3 (not visible count)");

        // Verify no data corruption
        long totalComments = commentRepository.findByPostId(testPost.getId()).size();
        assertEquals(3, totalComments, "All 3 comments should still exist (soft delete)");
    }

    /**
     * Test: Consistency persists after retrieval
     * Verifies: Consistency is not temporary, data stays consistent
     */
    @Test
    void testConsistencyPersistsAfterRetrieval() {
        CreateCommentRequest req = new CreateCommentRequest("Persistence check comment");
        postsService.addComment(testPost.getId(), req, testUser.getId());

        // Retrieve multiple times
        for (int i = 0; i < 5; i++) {
            Post post = postRepository.findById(testPost.getId()).orElseThrow();
            long actual = commentRepository.findByPostIdAndHiddenFalse(testPost.getId()).size();

            assertEquals(1, post.getCommentCount(),
                "Count should persist (retrieval #" + i + ")");
            assertEquals(1, actual,
                "Actual should persist (retrieval #" + i + ")");
            assertEquals(post.getCommentCount(), actual,
                "Consistency should persist (retrieval #" + i + ")");
        }
    }
}
