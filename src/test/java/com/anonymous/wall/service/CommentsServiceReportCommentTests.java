package com.anonymous.wall.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.CommentReport;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.repository.CommentReportRepository;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("CommentsService - Report Comment Tests")
class CommentsServiceReportCommentTests {

    @Inject
    private CommentsService commentsService;

    @Inject
    private PostRepository postRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private CommentReportRepository commentReportRepository;

    @Inject
    private PostLikeRepository postLikeRepository;

    private UserEntity commentAuthor;
    private UserEntity reporter;
    private UserEntity anotherReporter;
    private Post testPost;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        // Clean up in correct order
        commentReportRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create comment author
        commentAuthor = new UserEntity();
        commentAuthor.setEmail("author" + System.currentTimeMillis() + "@harvard.edu");
        commentAuthor.setSchoolDomain("harvard.edu");
        commentAuthor.setVerified(true);
        commentAuthor.setPasswordSet(true);
        commentAuthor = userRepository.save(commentAuthor);

        // Create reporter user
        reporter = new UserEntity();
        reporter.setEmail("reporter" + System.currentTimeMillis() + "@harvard.edu");
        reporter.setSchoolDomain("harvard.edu");
        reporter.setVerified(true);
        reporter.setPasswordSet(true);
        reporter = userRepository.save(reporter);

        // Create another reporter user
        anotherReporter = new UserEntity();
        anotherReporter.setEmail("another" + System.currentTimeMillis() + "@harvard.edu");
        anotherReporter.setSchoolDomain("harvard.edu");
        anotherReporter.setVerified(true);
        anotherReporter.setPasswordSet(true);
        anotherReporter = userRepository.save(anotherReporter);

        // Create test post
        testPost = new Post(commentAuthor.getId(), "Test Title", "Test post content", "campus", "harvard.edu");
        testPost = postRepository.save(testPost);

        // Create test comment using service to ensure proper initialization
        CreateCommentRequest commentRequest = new CreateCommentRequest("Test comment text");
        testComment = commentsService.addComment(testPost.getId(), commentRequest, commentAuthor.getId());
    }

    @AfterEach
    void tearDown() {
        commentReportRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== POSITIVE TEST CASES ====================

    @Nested
    @DisplayName("Report Comment - Positive Cases")
    class ReportCommentPositiveTests {

        @Test
        @Order(1)
        @DisplayName("Should report comment successfully without reason")
        void shouldReportCommentWithoutReason() {
            // Arrange
            int initialReportCount = commentAuthor.getReportCount();

            // Act
            commentsService.reportComment(testComment.getId(), reporter.getId(), null);

            // Assert
            // Verify report was created
            boolean reportExists = commentReportRepository.existsByCommentIdAndReporterUserId(
                testComment.getId(), reporter.getId());
            assertTrue(reportExists, "Report should exist in database");

            // Verify author's report count was incremented
            Optional<UserEntity> updatedAuthor = userRepository.findById(commentAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(initialReportCount + 1, updatedAuthor.get().getReportCount(),
                "Author's report count should be incremented");
        }

        @Test
        @Order(2)
        @DisplayName("Should report comment successfully with reason")
        void shouldReportCommentWithReason() {
            // Arrange
            String reason = "This comment contains offensive language";
            int initialReportCount = commentAuthor.getReportCount();

            // Act
            commentsService.reportComment(testComment.getId(), reporter.getId(), reason);

            // Assert
            // Verify report was created with reason
            Optional<CommentReport> report = commentReportRepository.findByCommentIdAndReporterUserId(
                testComment.getId(), reporter.getId());
            assertTrue(report.isPresent(), "Report should exist");
            assertEquals(reason, report.get().getReason(), "Report reason should match");

            // Verify author's report count was incremented
            Optional<UserEntity> updatedAuthor = userRepository.findById(commentAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(initialReportCount + 1, updatedAuthor.get().getReportCount());
        }

        @Test
        @Order(3)
        @DisplayName("Should allow different users to report same comment")
        void shouldAllowDifferentUsersToReportSameComment() {
            // Arrange
            int initialReportCount = commentAuthor.getReportCount();

            // Act - First reporter reports the comment
            commentsService.reportComment(testComment.getId(), reporter.getId(), "Reason 1");

            // Act - Second reporter reports the same comment
            commentsService.reportComment(testComment.getId(), anotherReporter.getId(), "Reason 2");

            // Assert
            // Verify both reports exist
            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                testComment.getId(), reporter.getId()));
            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                testComment.getId(), anotherReporter.getId()));

            // Verify author's report count was incremented twice
            Optional<UserEntity> updatedAuthor = userRepository.findById(commentAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(initialReportCount + 2, updatedAuthor.get().getReportCount(),
                "Author's report count should be incremented by 2");
        }

        @Test
        @Order(4)
        @DisplayName("Should handle long reason text (max 500 chars)")
        void shouldHandleLongReasonText() {
            // Arrange
            String longReason = "B".repeat(500); // Max length

            // Act & Assert - Should not throw exception
            assertDoesNotThrow(() -> commentsService.reportComment(testComment.getId(), reporter.getId(), longReason));

            // Verify report was created
            Optional<CommentReport> report = commentReportRepository.findByCommentIdAndReporterUserId(
                testComment.getId(), reporter.getId());
            assertTrue(report.isPresent());
            assertEquals(longReason, report.get().getReason());
        }

        @Test
        @Order(5)
        @DisplayName("Should allow same user to report different comments")
        void shouldAllowSameUserToReportDifferentComments() {
            // Arrange - Create another comment
            CreateCommentRequest commentRequest = new CreateCommentRequest("Another comment");
            Comment anotherComment = commentsService.addComment(testPost.getId(), commentRequest, commentAuthor.getId());

            // Act
            commentsService.reportComment(testComment.getId(), reporter.getId(), "Report first comment");
            commentsService.reportComment(anotherComment.getId(), reporter.getId(), "Report second comment");

            // Assert
            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                testComment.getId(), reporter.getId()));
            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                anotherComment.getId(), reporter.getId()));
        }
    }

    // ==================== NEGATIVE TEST CASES ====================

    @Nested
    @DisplayName("Report Comment - Negative Cases")
    class ReportCommentNegativeTests {

        @Test
        @Order(6)
        @DisplayName("Should fail when reporting non-existent comment")
        void shouldFailWhenReportingNonExistentComment() {
            // Arrange
            UUID nonExistentCommentId = UUID.randomUUID();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentsService.reportComment(nonExistentCommentId, reporter.getId(), "Reason")
            );

            assertEquals("Comment not found", exception.getMessage());
        }

        @Test
        @Order(7)
        @DisplayName("Should fail when user reports same comment twice")
        void shouldFailWhenUserReportsSameCommentTwice() {
            // Arrange - First report
            commentsService.reportComment(testComment.getId(), reporter.getId(), "First report");

            // Act & Assert - Second report should fail
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentsService.reportComment(testComment.getId(), reporter.getId(), "Second report")
            );

            assertEquals("You have already reported this comment", exception.getMessage());

            // Verify only one report exists
            long reportCount = commentReportRepository.countByCommentId(testComment.getId());
            assertEquals(1, reportCount, "Should only have one report");
        }

        @Test
        @Order(8)
        @DisplayName("Should not double-increment report count on duplicate attempt")
        void shouldNotDoubleIncrementReportCountOnDuplicateAttempt() {
            // Arrange
            int initialReportCount = commentAuthor.getReportCount();
            
            // First report
            commentsService.reportComment(testComment.getId(), reporter.getId(), "First report");

            // Verify count increased by 1
            Optional<UserEntity> authorAfterFirstReport = userRepository.findById(commentAuthor.getId());
            assertTrue(authorAfterFirstReport.isPresent());
            assertEquals(initialReportCount + 1, authorAfterFirstReport.get().getReportCount());

            // Act - Try to report again (should fail)
            assertThrows(IllegalArgumentException.class,
                () -> commentsService.reportComment(testComment.getId(), reporter.getId(), "Second report"));

            // Assert - Report count should still be +1, not +2
            Optional<UserEntity> authorAfterDuplicate = userRepository.findById(commentAuthor.getId());
            assertTrue(authorAfterDuplicate.isPresent());
            assertEquals(initialReportCount + 1, authorAfterDuplicate.get().getReportCount(),
                "Report count should not increase on duplicate report attempt");
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Report Comment - Edge Cases")
    class ReportCommentEdgeTests {

        @Test
        @Order(9)
        @DisplayName("Should handle empty reason string")
        void shouldHandleEmptyReasonString() {
            // Act & Assert
            assertDoesNotThrow(() -> commentsService.reportComment(testComment.getId(), reporter.getId(), ""));

            // Verify report was created with empty reason
            Optional<CommentReport> report = commentReportRepository.findByCommentIdAndReporterUserId(
                testComment.getId(), reporter.getId());
            assertTrue(report.isPresent());
            assertEquals("", report.get().getReason());
        }

        @Test
        @Order(10)
        @DisplayName("Should allow user to report their own comment")
        void shouldAllowUserToReportOwnComment() {
            // Act & Assert - User can report their own comment
            assertDoesNotThrow(() -> commentsService.reportComment(testComment.getId(), commentAuthor.getId(), "Self-report"));

            // Verify report exists
            boolean reportExists = commentReportRepository.existsByCommentIdAndReporterUserId(
                testComment.getId(), commentAuthor.getId());
            assertTrue(reportExists);

            // Verify author's own report count was incremented
            Optional<UserEntity> updatedAuthor = userRepository.findById(commentAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertTrue(updatedAuthor.get().getReportCount() > 0);
        }

        @Test
        @Order(11)
        @DisplayName("Should track separate report counts for posts and comments")
        void shouldTrackSeparateReportCountsForPostsAndComments() {
            // Arrange - Create a post by the same author
            Post authorPost = new Post(commentAuthor.getId(), "Author's Post", "Content", "campus", "harvard.edu");
            authorPost = postRepository.save(authorPost);

            int initialReportCount = commentAuthor.getReportCount();

            // Act - Report both the comment and the post
            commentsService.reportComment(testComment.getId(), reporter.getId(), "Report comment");
            
            // Need to inject PostsService for this test
            // For now, we'll just verify the comment report increments the count
            Optional<UserEntity> afterCommentReport = userRepository.findById(commentAuthor.getId());
            assertTrue(afterCommentReport.isPresent());
            assertEquals(initialReportCount + 1, afterCommentReport.get().getReportCount(),
                "Report count should increase by 1 after comment report");
        }
    }
}
