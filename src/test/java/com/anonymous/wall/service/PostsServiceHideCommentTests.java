package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PostsService - Hide/Unhide Comment Tests")
class PostsServiceHideCommentTests {

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

    private UserEntity testUserCampus;
    private UserEntity testUserDifferentSchool;
    private Post campusPost;
    private Post nationalPost;
    private Comment campusComment;
    private Comment nationalComment;
    private UUID userCampusId;
    private UUID userDifferentSchoolId;

    @BeforeEach
    void setUp() {
        // Clean up
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUserCampus = new UserEntity();
        testUserCampus.setEmail("student" + System.currentTimeMillis() + "@harvard.edu");
        testUserCampus.setSchoolDomain("harvard.edu");
        testUserCampus.setVerified(true);
        testUserCampus.setPasswordSet(true);
        testUserCampus = userRepository.save(testUserCampus);
        userCampusId = testUserCampus.getId();

        testUserDifferentSchool = new UserEntity();
        testUserDifferentSchool.setEmail("student" + System.currentTimeMillis() + "@mit.edu");
        testUserDifferentSchool.setSchoolDomain("mit.edu");
        testUserDifferentSchool.setVerified(true);
        testUserDifferentSchool.setPasswordSet(true);
        testUserDifferentSchool = userRepository.save(testUserDifferentSchool);
        userDifferentSchoolId = testUserDifferentSchool.getId();

        // Create test posts
        campusPost = new Post(userCampusId, "Harvard campus post", "campus", "harvard.edu");
        campusPost = postRepository.save(campusPost);

        nationalPost = new Post(userCampusId, "National post", "national", null);
        nationalPost = postRepository.save(nationalPost);

        // Create test comments
        campusComment = new Comment(campusPost.getId(), userCampusId, "Campus comment");
        campusComment = commentRepository.save(campusComment);

        nationalComment = new Comment(nationalPost.getId(), userCampusId, "National comment");
        nationalComment = commentRepository.save(nationalComment);
    }

    @AfterEach
    void tearDown() {
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ================= POSITIVE TEST CASES =================

    @Nested
    @DisplayName("Hide Comment - Positive Cases")
    class HideCommentPositiveTests {

        @Test
        @Order(1)
        @DisplayName("Should hide own comment on campus post")
        void shouldHideOwnCommentOnCampusPost() {
            assertFalse(campusComment.isHidden(), "Comment should initially be visible");

            Comment result = postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            assertTrue(result.isHidden(), "Comment should be hidden");

            // Verify in database
            Optional<Comment> dbComment = commentRepository.findById(campusComment.getId());
            assertTrue(dbComment.isPresent());
            assertTrue(dbComment.get().isHidden());
        }

        @Test
        @Order(2)
        @DisplayName("Should hide own comment on national post")
        void shouldHideOwnCommentOnNationalPost() {
            assertFalse(nationalComment.isHidden(), "Comment should initially be visible");

            Comment result = postsService.hideComment(nationalPost.getId(), nationalComment.getId(), userCampusId);

            assertTrue(result.isHidden(), "Comment should be hidden");

            Optional<Comment> dbComment = commentRepository.findById(nationalComment.getId());
            assertTrue(dbComment.isPresent());
            assertTrue(dbComment.get().isHidden());
        }

        @Test
        @Order(3)
        @DisplayName("Should handle hiding already hidden comment (idempotent)")
        void shouldHandleHidingAlreadyHiddenComment() {
            // First hide
            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Hide again
            Comment result = postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            assertTrue(result.isHidden(), "Comment should still be hidden");

            Optional<Comment> dbComment = commentRepository.findById(campusComment.getId());
            assertTrue(dbComment.isPresent());
            assertTrue(dbComment.get().isHidden());
        }

        @Test
        @Order(4)
        @DisplayName("Should preserve comment data when hiding")
        void shouldPreserveCommentDataWhenHiding() {
            String originalText = campusComment.getText();
            Long originalPostId = campusComment.getPostId();
            UUID originalUserId = campusComment.getUserId();

            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            Optional<Comment> result = commentRepository.findById(campusComment.getId());
            assertTrue(result.isPresent());
            assertEquals(originalText, result.get().getText(), "Comment text should be preserved");
            assertEquals(originalPostId, result.get().getPostId(), "Post ID should be preserved");
            assertEquals(originalUserId, result.get().getUserId(), "User ID should be preserved");
        }

        @Test
        @Order(5)
        @DisplayName("Should increment version when hiding comment")
        void shouldIncrementVersionWhenHiding() {
            Long initialVersion = campusComment.getVersion();

            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            Optional<Comment> result = commentRepository.findById(campusComment.getId());
            assertTrue(result.isPresent());
            assertTrue(result.get().getVersion() > initialVersion, "Version should be incremented");
        }
    }

    @Nested
    @DisplayName("Unhide Comment - Positive Cases")
    class UnhideCommentPositiveTests {

        @Test
        @Order(10)
        @DisplayName("Should unhide own hidden comment on campus post")
        void shouldUnhideOwnHiddenCommentOnCampusPost() {
            // First hide
            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);
            Optional<Comment> hidden = commentRepository.findById(campusComment.getId());
            assertTrue(hidden.isPresent() && hidden.get().isHidden(), "Comment should be hidden after hide operation");

            // Then unhide
            Comment result = postsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            assertFalse(result.isHidden(), "Comment should be visible after unhide");

            Optional<Comment> dbComment = commentRepository.findById(campusComment.getId());
            assertTrue(dbComment.isPresent());
            assertFalse(dbComment.get().isHidden());
        }

        @Test
        @Order(11)
        @DisplayName("Should unhide own hidden comment on national post")
        void shouldUnhideOwnHiddenCommentOnNationalPost() {
            // First hide
            postsService.hideComment(nationalPost.getId(), nationalComment.getId(), userCampusId);

            // Then unhide
            Comment result = postsService.unhideComment(nationalPost.getId(), nationalComment.getId(), userCampusId);

            assertFalse(result.isHidden(), "Comment should be visible after unhide");

            Optional<Comment> dbComment = commentRepository.findById(nationalComment.getId());
            assertTrue(dbComment.isPresent());
            assertFalse(dbComment.get().isHidden());
        }

        @Test
        @Order(12)
        @DisplayName("Should handle unhiding already visible comment (idempotent)")
        void shouldHandleUnhidingAlreadyVisibleComment() {
            // Try to unhide without hiding first
            Comment result = postsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            assertFalse(result.isHidden(), "Comment should remain visible");

            Optional<Comment> dbComment = commentRepository.findById(campusComment.getId());
            assertTrue(dbComment.isPresent());
            assertFalse(dbComment.get().isHidden());
        }

        @Test
        @Order(13)
        @DisplayName("Should support multiple hide/unhide cycles")
        void shouldSupportMultipleHideUnhideCycles() {
            // First cycle
            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);
            postsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Second cycle
            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);
            Comment result = postsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            assertFalse(result.isHidden(), "Comment should be visible after final unhide");

            Optional<Comment> dbComment = commentRepository.findById(campusComment.getId());
            assertTrue(dbComment.isPresent());
            assertFalse(dbComment.get().isHidden());
        }

        @Test
        @Order(14)
        @DisplayName("Should preserve comment data when unhiding")
        void shouldPreserveCommentDataWhenUnhiding() {
            String originalText = campusComment.getText();
            Long originalPostId = campusComment.getPostId();
            UUID originalUserId = campusComment.getUserId();

            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);
            postsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            Optional<Comment> result = commentRepository.findById(campusComment.getId());
            assertTrue(result.isPresent());
            assertEquals(originalText, result.get().getText(), "Comment text should be preserved");
            assertEquals(originalPostId, result.get().getPostId(), "Post ID should be preserved");
            assertEquals(originalUserId, result.get().getUserId(), "User ID should be preserved");
        }
    }

    // ================= NEGATIVE TEST CASES =================

    @Nested
    @DisplayName("Hide Comment - Negative Cases")
    class HideCommentNegativeTests {

        @Test
        @Order(20)
        @DisplayName("Should throw exception when trying to hide another user's comment")
        void shouldThrowExceptionWhenHidingAnotherUserComment() {
            // Create a comment by userDifferentSchool on a national post (so no access issues)
            Comment otherUserComment = new Comment(nationalPost.getId(), userDifferentSchoolId, "Comment by other user");
            final Comment savedOtherUserComment = commentRepository.save(otherUserComment);

            // Now userCampus tries to hide it
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.hideComment(nationalPost.getId(), savedOtherUserComment.getId(), userCampusId)
            );

            assertTrue(exception.getMessage().contains("hide your own comments"));

            // Verify comment is still visible
            Optional<Comment> dbComment = commentRepository.findById(savedOtherUserComment.getId());
            assertTrue(dbComment.isPresent());
            assertFalse(dbComment.get().isHidden());
        }

        @Test
        @Order(21)
        @DisplayName("Should throw exception for non-existent comment")
        void shouldThrowExceptionForNonExistentComment() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.hideComment(campusPost.getId(), 99999L, userCampusId)
            );

            assertTrue(exception.getMessage().contains("not found"));
        }

        @Test
        @Order(22)
        @DisplayName("Should throw exception when post does not exist")
        void shouldThrowExceptionWhenPostDoesNotExist() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.hideComment(99999L, campusComment.getId(), userCampusId)
            );

            assertTrue(exception.getMessage().contains("not found"));
        }

        @Test
        @Order(23)
        @DisplayName("Should throw exception when comment does not belong to post")
        void shouldThrowExceptionWhenCommentDoesNotBelongToPost() {
            // Comment belongs to nationalPost, try to hide as if it belongs to campusPost
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.hideComment(campusPost.getId(), nationalComment.getId(), userCampusId)
            );

            assertTrue(exception.getMessage().contains("does not belong to this post"));

            // Verify comment is still visible
            Optional<Comment> dbComment = commentRepository.findById(nationalComment.getId());
            assertTrue(dbComment.isPresent());
            assertFalse(dbComment.get().isHidden());
        }

        @Test
        @Order(24)
        @DisplayName("Should throw exception when user does not have post access")
        void shouldThrowExceptionWhenUserDoesNotHavePostAccess() {
            // MIT student trying to hide comment on Harvard campus post
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.hideComment(campusPost.getId(), campusComment.getId(), userDifferentSchoolId)
            );

            // Should be either access error or hide own comments error (access is checked first)
            assertTrue(
                exception.getMessage().contains("access") ||
                exception.getMessage().contains("hide your own comments")
            );
        }
    }

    @Nested
    @DisplayName("Unhide Comment - Negative Cases")
    class UnhideCommentNegativeTests {

        @Test
        @Order(30)
        @DisplayName("Should throw exception when trying to unhide another user's comment")
        void shouldThrowExceptionWhenUnhidingAnotherUserComment() {
            // Create a hidden comment by userDifferentSchool on a national post
            Comment otherUserComment = new Comment(nationalPost.getId(), userDifferentSchoolId, "Comment by other user");
            otherUserComment = commentRepository.save(otherUserComment);
            otherUserComment.setHidden(true);
            final Comment savedOtherUserComment = commentRepository.save(otherUserComment);

            // Try to unhide as different user (userCampus)
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.unhideComment(nationalPost.getId(), savedOtherUserComment.getId(), userCampusId)
            );

            assertTrue(exception.getMessage().contains("unhide your own comments"));

            // Verify comment is still hidden
            Optional<Comment> dbComment = commentRepository.findById(savedOtherUserComment.getId());
            assertTrue(dbComment.isPresent());
            assertTrue(dbComment.get().isHidden());
        }

        @Test
        @Order(31)
        @DisplayName("Should throw exception for non-existent comment on unhide")
        void shouldThrowExceptionForNonExistentCommentOnUnhide() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.unhideComment(campusPost.getId(), 99999L, userCampusId)
            );

            assertTrue(exception.getMessage().contains("not found"));
        }

        @Test
        @Order(32)
        @DisplayName("Should throw exception when post does not exist on unhide")
        void shouldThrowExceptionWhenPostDoesNotExistOnUnhide() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.unhideComment(99999L, campusComment.getId(), userCampusId)
            );

            assertTrue(exception.getMessage().contains("not found"));
        }

        @Test
        @Order(33)
        @DisplayName("Should throw exception when comment does not belong to post on unhide")
        void shouldThrowExceptionWhenCommentDoesNotBelongToPostOnUnhide() {
            // Hide first
            postsService.hideComment(nationalPost.getId(), nationalComment.getId(), userCampusId);

            // Try to unhide as if it belongs to campusPost
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.unhideComment(campusPost.getId(), nationalComment.getId(), userCampusId)
            );

            assertTrue(exception.getMessage().contains("does not belong to this post"));

            // Verify comment is still hidden
            Optional<Comment> dbComment = commentRepository.findById(nationalComment.getId());
            assertTrue(dbComment.isPresent());
            assertTrue(dbComment.get().isHidden());
        }

        @Test
        @Order(34)
        @DisplayName("Should throw exception when user does not have post access on unhide")
        void shouldThrowExceptionWhenUserDoesNotHavePostAccessOnUnhide() {
            // MIT student trying to unhide comment on Harvard campus post
            // First hide as owner
            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Then try to unhide as different user
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.unhideComment(campusPost.getId(), campusComment.getId(), userDifferentSchoolId)
            );

            assertTrue(
                exception.getMessage().contains("access") ||
                exception.getMessage().contains("unhide your own comments")
            );
        }
    }

    // ================= EDGE CASES =================

    @Nested
    @DisplayName("Hide/Unhide Comment - Edge Cases")
    class HideUnhideCommentEdgeCases {

        @Test
        @Order(40)
        @DisplayName("Should handle hiding comment with special characters")
        void shouldHandleHidingCommentWithSpecialCharacters() {
            String specialText = "Comment with 🎉 emoji @mention #hashtag ñ character";
            Comment specialComment = new Comment(campusPost.getId(), userCampusId, specialText);
            specialComment = commentRepository.save(specialComment);

            Comment result = postsService.hideComment(campusPost.getId(), specialComment.getId(), userCampusId);

            assertTrue(result.isHidden());
            assertEquals(specialText, result.getText(), "Special characters should be preserved");
        }

        @Test
        @Order(41)
        @DisplayName("Should handle hiding comment with very long text")
        void shouldHandleHidingCommentWithVeryLongText() {
            String longText = "X".repeat(5000);
            Comment longComment = new Comment(campusPost.getId(), userCampusId, longText);
            longComment = commentRepository.save(longComment);

            Comment result = postsService.hideComment(campusPost.getId(), longComment.getId(), userCampusId);

            assertTrue(result.isHidden());
            assertEquals(5000, result.getText().length(), "Long text should be preserved");
        }

        @Test
        @Order(42)
        @DisplayName("Should correctly filter hidden comments from getComments")
        void shouldFilterHiddenCommentsFromGetComments() {
            // Create multiple comments
            Comment comment2 = new Comment(campusPost.getId(), userCampusId, "Comment 2");
            comment2 = commentRepository.save(comment2);
            Comment comment3 = new Comment(campusPost.getId(), userCampusId, "Comment 3");
            comment3 = commentRepository.save(comment3);

            final Long comment2Id = comment2.getId();
            final Long comment3Id = comment3.getId();
            final Long campusCommentId = campusComment.getId();

            // Hide one comment
            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Get comments should exclude hidden ones
            List<Comment> visibleComments = postsService.getComments(campusPost.getId());

            assertEquals(2, visibleComments.size(), "Should only return visible comments");
            assertTrue(visibleComments.stream().anyMatch(c -> c.getId().equals(comment2Id)));
            assertTrue(visibleComments.stream().anyMatch(c -> c.getId().equals(comment3Id)));
            assertFalse(visibleComments.stream().anyMatch(c -> c.getId().equals(campusCommentId)));
        }

        @Test
        @Order(43)
        @DisplayName("Should handle hiding comment and verifying database state")
        void shouldHandleHidingCommentAndVerifyDatabaseState() {
            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Verify database directly
            Optional<Comment> dbComment = commentRepository.findById(campusComment.getId());
            assertTrue(dbComment.isPresent());
            assertTrue(dbComment.get().isHidden());

            // Verify it's not in the visible comments list
            List<Comment> visibleComments = postsService.getComments(campusPost.getId());
            assertFalse(visibleComments.stream().anyMatch(c -> c.getId().equals(campusComment.getId())));
        }

        @Test
        @Order(44)
        @DisplayName("Should maintain comment count when hiding")
        void shouldMaintainCommentCountWhenHiding() {
            // Add a few more comments
            Comment comment2 = new Comment(campusPost.getId(), userCampusId, "Comment 2");
            comment2 = commentRepository.save(comment2);

            int initialCommentCount = postsService.getComments(campusPost.getId()).size();

            // Hide one comment
            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // The post's comment_count field may not change (it tracks all comments including hidden)
            // But the visible comments should decrease
            int visibleCommentCount = postsService.getComments(campusPost.getId()).size();
            assertEquals(initialCommentCount - 1, visibleCommentCount, "Visible comment count should decrease");
        }

        @Test
        @Order(45)
        @DisplayName("Should handle unhiding and immediately hiding again")
        void shouldHandleUnhidingAndImmediatelyHidinAgain() {
            postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);
            postsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Immediately hide again
            Comment result = postsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            assertTrue(result.isHidden());

            Optional<Comment> dbComment = commentRepository.findById(campusComment.getId());
            assertTrue(dbComment.isPresent());
            assertTrue(dbComment.get().isHidden());
        }

        @Test
        @Order(46)
        @DisplayName("Should handle hiding when post is from different user but same school")
        void shouldAllowHidingCommentWhenPostFromDifferentUserButSameSchool() {
            // Create another Harvard user
            UserEntity user2 = new UserEntity();
            user2.setEmail("student2@harvard.edu");
            user2.setSchoolDomain("harvard.edu");
            user2.setVerified(true);
            user2.setPasswordSet(true);
            user2 = userRepository.save(user2);

            // Create a post by user2
            Post post2 = new Post(user2.getId(), "Post by user2", "campus", "harvard.edu");
            post2 = postRepository.save(post2);

            // User1 adds a comment to user2's post
            Comment comment = new Comment(post2.getId(), userCampusId, "Comment by user1");
            comment = commentRepository.save(comment);

            // User1 should be able to hide their own comment even though post is by user2
            Comment result = postsService.hideComment(post2.getId(), comment.getId(), userCampusId);

            assertTrue(result.isHidden());
        }

        @Test
        @Order(47)
        @DisplayName("Should validate post visibility before hiding")
        void shouldValidatePostVisibilityBeforeHiding() {
            // Create a post by different school
            Post mitPost = new Post(userDifferentSchoolId, "MIT post", "campus", "mit.edu");
            mitPost = postRepository.save(mitPost);

            // Create a comment on MIT post
            Comment mitComment = new Comment(mitPost.getId(), userDifferentSchoolId, "MIT comment");
            mitComment = commentRepository.save(mitComment);

            final Long mitPostId = mitPost.getId();
            final Long mitCommentId = mitComment.getId();

            // Harvard student should not be able to hide comment on MIT post (no access)
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.hideComment(mitPostId, mitCommentId, userCampusId)
            );

            assertTrue(exception.getMessage().contains("access"));
        }
    }
}
