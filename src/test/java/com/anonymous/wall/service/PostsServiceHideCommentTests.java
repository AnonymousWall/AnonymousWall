package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
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
    private CommentsService commentsService;

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
        campusPost = new Post(userCampusId, "Title", "Harvard campus post", "campus", "harvard.edu");
        campusPost = postRepository.save(campusPost);

        nationalPost = new Post(userCampusId, "Title", "National post", "national", null);
        nationalPost = postRepository.save(nationalPost);

        // Create test comments
        // Use service to ensure comment counts are properly updated
        campusComment = commentsService.addComment(campusPost.getId(),
            new com.anonymous.wall.model.CreateCommentRequest("Campus comment"), userCampusId);

        nationalComment = commentsService.addComment(nationalPost.getId(),
            new com.anonymous.wall.model.CreateCommentRequest("National comment"), userCampusId);
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

            Comment result = commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

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

            Comment result = commentsService.hideComment(nationalPost.getId(), nationalComment.getId(), userCampusId);

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
            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Hide again
            Comment result = commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

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
            UUID originalPostId = campusComment.getPostId();
            UUID originalUserId = campusComment.getUserId();

            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

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

            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

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
            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);
            Optional<Comment> hidden = commentRepository.findById(campusComment.getId());
            assertTrue(hidden.isPresent() && hidden.get().isHidden(), "Comment should be hidden after hide operation");

            // Then unhide
            Comment result = commentsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

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
            commentsService.hideComment(nationalPost.getId(), nationalComment.getId(), userCampusId);

            // Then unhide
            Comment result = commentsService.unhideComment(nationalPost.getId(), nationalComment.getId(), userCampusId);

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
            Comment result = commentsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

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
            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);
            commentsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Second cycle
            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);
            Comment result = commentsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

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
            UUID originalPostId = campusComment.getPostId();
            UUID originalUserId = campusComment.getUserId();

            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);
            commentsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

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
                () -> commentsService.hideComment(nationalPost.getId(), savedOtherUserComment.getId(), userCampusId)
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
                () -> commentsService.hideComment(campusPost.getId(), UUID.randomUUID(), userCampusId)
            );

            assertTrue(exception.getMessage().contains("not found"));
        }

        @Test
        @Order(22)
        @DisplayName("Should throw exception when post does not exist")
        void shouldThrowExceptionWhenPostDoesNotExist() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentsService.hideComment(UUID.randomUUID(), campusComment.getId(), userCampusId)
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
                () -> commentsService.hideComment(campusPost.getId(), nationalComment.getId(), userCampusId)
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
                () -> commentsService.hideComment(campusPost.getId(), campusComment.getId(), userDifferentSchoolId)
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
                () -> commentsService.unhideComment(nationalPost.getId(), savedOtherUserComment.getId(), userCampusId)
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
                () -> commentsService.unhideComment(campusPost.getId(), UUID.randomUUID(), userCampusId)
            );

            assertTrue(exception.getMessage().contains("not found"));
        }

        @Test
        @Order(32)
        @DisplayName("Should throw exception when post does not exist on unhide")
        void shouldThrowExceptionWhenPostDoesNotExistOnUnhide() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentsService.unhideComment(UUID.randomUUID(), campusComment.getId(), userCampusId)
            );

            assertTrue(exception.getMessage().contains("not found"));
        }

        @Test
        @Order(33)
        @DisplayName("Should throw exception when comment does not belong to post on unhide")
        void shouldThrowExceptionWhenCommentDoesNotBelongToPostOnUnhide() {
            // Hide first
            commentsService.hideComment(nationalPost.getId(), nationalComment.getId(), userCampusId);

            // Try to unhide as if it belongs to campusPost
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentsService.unhideComment(campusPost.getId(), nationalComment.getId(), userCampusId)
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
            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Then try to unhide as different user
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentsService.unhideComment(campusPost.getId(), campusComment.getId(), userDifferentSchoolId)
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
            Comment specialComment = commentsService.addComment(campusPost.getId(),
                new com.anonymous.wall.model.CreateCommentRequest(specialText), userCampusId);

            Comment result = commentsService.hideComment(campusPost.getId(), specialComment.getId(), userCampusId);

            assertTrue(result.isHidden());
            assertEquals(specialText, result.getText(), "Special characters should be preserved");
        }

        @Test
        @Order(41)
        @DisplayName("Should handle hiding comment with very long text")
        void shouldHandleHidingCommentWithVeryLongText() {
            String longText = "X".repeat(5000);
            Comment longComment = commentsService.addComment(campusPost.getId(),
                new com.anonymous.wall.model.CreateCommentRequest(longText), userCampusId);

            Comment result = commentsService.hideComment(campusPost.getId(), longComment.getId(), userCampusId);

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

            final UUID comment2Id = comment2.getId();
            final UUID comment3Id = comment3.getId();
            final UUID campusCommentId = campusComment.getId();

            // Hide one comment
            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Get comments should exclude hidden ones
            Pageable pageable = Pageable.from(0, 100);
            Page<Comment> visibleCommentsPage = commentsService.getCommentsWithPagination(campusPost.getId(), pageable, SortBy.NEWEST);
            List<Comment> visibleComments = visibleCommentsPage.getContent();

            assertEquals(2, visibleComments.size(), "Should only return visible comments");
            assertTrue(visibleComments.stream().anyMatch(c -> c.getId().equals(comment2Id)));
            assertTrue(visibleComments.stream().anyMatch(c -> c.getId().equals(comment3Id)));
            assertFalse(visibleComments.stream().anyMatch(c -> c.getId().equals(campusCommentId)));
        }

        @Test
        @Order(43)
        @DisplayName("Should handle hiding comment and verifying database state")
        void shouldHandleHidingCommentAndVerifyDatabaseState() {
            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Verify database directly
            Optional<Comment> dbComment = commentRepository.findById(campusComment.getId());
            assertTrue(dbComment.isPresent());
            assertTrue(dbComment.get().isHidden());

            // Verify it's not in the visible comments list
            Pageable pageable = Pageable.from(0, 100);
            Page<Comment> visibleCommentsPage = commentsService.getCommentsWithPagination(campusPost.getId(), pageable, SortBy.NEWEST);
            List<Comment> visibleComments = visibleCommentsPage.getContent();
            assertFalse(visibleComments.stream().anyMatch(c -> c.getId().equals(campusComment.getId())));
        }

        @Test
        @Order(44)
        @DisplayName("Should decrement comment count when hiding")
        void shouldDecrementCommentCountWhenHiding() {
            // Add more comments via service to ensure comment counts are updated
            commentsService.addComment(campusPost.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("Comment 2"), userCampusId);

            Post postBefore = postRepository.findById(campusPost.getId()).get();
            int initialCount = postBefore.getCommentCount();

            // Hide one comment
            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Verify comment count is decremented (soft-delete behavior)
            Post postAfter = postRepository.findById(campusPost.getId()).get();
            assertEquals(initialCount - 1, postAfter.getCommentCount(),
                "Post comment count should be decremented when hiding");

            // Verify visible comments also decreased
            Pageable pageable = Pageable.from(0, 100);
            Page<Comment> visibleCommentsPage = commentsService.getCommentsWithPagination(campusPost.getId(), pageable, SortBy.NEWEST);
            int visibleCommentCount = visibleCommentsPage.getContent().size();
            assertEquals(initialCount - 1, visibleCommentCount,
                "Visible comment count should match post comment count");
        }

        @Test
        @Order(45)
        @DisplayName("Should increment comment count when unhiding")
        void shouldIncrementCommentCountWhenUnhiding() {
            // First hide a comment
            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            Post postAfterHide = postRepository.findById(campusPost.getId()).get();
            int countAfterHide = postAfterHide.getCommentCount();

            // Then unhide it
            commentsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Verify comment count is incremented back
            Post postAfterUnhide = postRepository.findById(campusPost.getId()).get();
            assertEquals(countAfterHide + 1, postAfterUnhide.getCommentCount(),
                "Post comment count should be incremented when unhiding");

            // Verify visible comments increased
            Pageable pageable = Pageable.from(0, 100);
            Page<Comment> visibleCommentsPage = commentsService.getCommentsWithPagination(campusPost.getId(), pageable, SortBy.NEWEST);
            int visibleCommentCount = visibleCommentsPage.getContent().size();
            assertEquals(countAfterHide + 1, visibleCommentCount,
                "Visible comment count should match post comment count");
        }

        @Test
        @Order(45)
        @DisplayName("Should handle unhiding and immediately hiding again")
        void shouldHandleUnhidingAndImmediatelyHidinAgain() {
            commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);
            commentsService.unhideComment(campusPost.getId(), campusComment.getId(), userCampusId);

            // Immediately hide again
            Comment result = commentsService.hideComment(campusPost.getId(), campusComment.getId(), userCampusId);

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
            Post post2 = new Post(user2.getId(), "Title", "Post by user2", "campus", "harvard.edu");
            post2 = postRepository.save(post2);

            // User1 adds a comment to user2's post
            Comment comment = new Comment(post2.getId(), userCampusId, "Comment by user1");
            comment = commentRepository.save(comment);

            // User1 should be able to hide their own comment even though post is by user2
            Comment result = commentsService.hideComment(post2.getId(), comment.getId(), userCampusId);

            assertTrue(result.isHidden());
        }

        @Test
        @Order(47)
        @DisplayName("Should validate post visibility before hiding")
        void shouldValidatePostVisibilityBeforeHiding() {
            // Create a post by different school
            Post mitPost = new Post(userDifferentSchoolId, "Title", "MIT post", "campus", "mit.edu");
            mitPost = postRepository.save(mitPost);

            // Create a comment on MIT post via service
            Comment mitComment = commentsService.addComment(mitPost.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("MIT comment"), userDifferentSchoolId);

            final UUID mitPostId = mitPost.getId();
            final UUID mitCommentId = mitComment.getId();

            // Harvard student should not be able to hide comment on MIT post (no access)
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentsService.hideComment(mitPostId, mitCommentId, userCampusId)
            );

            assertTrue(exception.getMessage().contains("access"));
        }
    }
}
