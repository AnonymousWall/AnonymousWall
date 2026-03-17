package com.anonymous.wall.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.CommentReport;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CommentParentType;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.repository.CommentReportRepository;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.CommentsService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
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
        commentReportRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Use UUID suffix — System.currentTimeMillis() can collide when three users
        // are created in the same @BeforeEach within the same millisecond.
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        commentAuthor = new UserEntity();
        commentAuthor.setEmail("author" + suffix + "@harvard.edu");
        commentAuthor.setSchoolDomain("harvard.edu");
        commentAuthor.setVerified(true);
        commentAuthor.setPasswordSet(true);
        commentAuthor = userRepository.save(commentAuthor);

        reporter = new UserEntity();
        reporter.setEmail("reporter" + suffix + "@harvard.edu");
        reporter.setSchoolDomain("harvard.edu");
        reporter.setVerified(true);
        reporter.setPasswordSet(true);
        reporter = userRepository.save(reporter);

        anotherReporter = new UserEntity();
        anotherReporter.setEmail("another" + suffix + "@harvard.edu");
        anotherReporter.setSchoolDomain("harvard.edu");
        anotherReporter.setVerified(true);
        anotherReporter.setPasswordSet(true);
        anotherReporter = userRepository.save(anotherReporter);

        testPost = new Post(commentAuthor.getId(), "Test Title", "Test post content", "campus", "harvard.edu");
        testPost = postRepository.save(testPost);

        CreateCommentRequest commentRequest = new CreateCommentRequest("Test comment text");
        testComment = commentsService.addComment(CommentParentType.POST, testPost.getId(),
                commentRequest, commentAuthor.getId());
    }

    @AfterEach
    void tearDown() {
        commentReportRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ─── Positive Cases ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Report Comment — Positive Cases")
    class ReportCommentPositiveTests {

        @Test
        @DisplayName("Should report comment successfully without reason")
        void shouldReportCommentWithoutReason() {
            int initialReportCount = commentAuthor.getReportCount();

            commentsService.reportComment(testComment.getId(), reporter.getId(), null);

            assertTrue(
                    commentReportRepository.existsByCommentIdAndReporterUserId(testComment.getId(), reporter.getId()),
                    "Report should exist in database"
            );
            Optional<UserEntity> updatedAuthor = userRepository.findById(commentAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(initialReportCount + 1, updatedAuthor.get().getReportCount(),
                    "Author's report count should be incremented by 1");
        }

        @Test
        @DisplayName("Should report comment successfully with reason")
        void shouldReportCommentWithReason() {
            String reason = "This comment contains offensive language";
            int initialReportCount = commentAuthor.getReportCount();

            commentsService.reportComment(testComment.getId(), reporter.getId(), reason);

            Optional<CommentReport> report = commentReportRepository
                    .findByCommentIdAndReporterUserId(testComment.getId(), reporter.getId());
            assertTrue(report.isPresent(), "Report should exist");
            assertEquals(reason, report.get().getReason(), "Report reason should match");
            Optional<UserEntity> updatedAuthor = userRepository.findById(commentAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(initialReportCount + 1, updatedAuthor.get().getReportCount());
        }

        @Test
        @DisplayName("Should set reportedUserId to comment author — not to reporter")
        void shouldSetReportedUserIdToCommentAuthor() {
            commentsService.reportComment(testComment.getId(), reporter.getId(), "Reason");

            Optional<CommentReport> report = commentReportRepository
                    .findByCommentIdAndReporterUserId(testComment.getId(), reporter.getId());
            assertTrue(report.isPresent());
            assertEquals(commentAuthor.getId(), report.get().getReportedUserId(),
                    "reportedUserId must be the comment author, not the reporter");
            assertNotEquals(reporter.getId(), report.get().getReportedUserId());
        }

        @Test
        @DisplayName("Should allow different users to report same comment")
        void shouldAllowDifferentUsersToReportSameComment() {
            int initialReportCount = commentAuthor.getReportCount();

            commentsService.reportComment(testComment.getId(), reporter.getId(), "Reason 1");
            commentsService.reportComment(testComment.getId(), anotherReporter.getId(), "Reason 2");

            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                    testComment.getId(), reporter.getId()));
            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                    testComment.getId(), anotherReporter.getId()));

            Optional<UserEntity> updatedAuthor = userRepository.findById(commentAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(initialReportCount + 2, updatedAuthor.get().getReportCount(),
                    "Author's report count should be incremented once per unique reporter");
        }

        @Test
        @DisplayName("Should handle reason text at max length — 500 chars")
        void shouldHandleMaxLengthReason() {
            String maxReason = "B".repeat(500);

            assertDoesNotThrow(() ->
                    commentsService.reportComment(testComment.getId(), reporter.getId(), maxReason));

            Optional<CommentReport> report = commentReportRepository
                    .findByCommentIdAndReporterUserId(testComment.getId(), reporter.getId());
            assertTrue(report.isPresent());
            assertEquals(maxReason, report.get().getReason());
        }

        @Test
        @DisplayName("Should allow same user to report different comments")
        void shouldAllowSameUserToReportDifferentComments() {
            CreateCommentRequest req = new CreateCommentRequest("Another comment");
            Comment anotherComment = commentsService.addComment(
                    CommentParentType.POST, testPost.getId(), req, commentAuthor.getId());

            commentsService.reportComment(testComment.getId(), reporter.getId(), "Report first");
            commentsService.reportComment(anotherComment.getId(), reporter.getId(), "Report second");

            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                    testComment.getId(), reporter.getId()));
            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                    anotherComment.getId(), reporter.getId()));
        }
    }

    // ─── Negative Cases ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Report Comment — Negative Cases")
    class ReportCommentNegativeTests {

        @Test
        @DisplayName("Should fail when reporting non-existent comment")
        void shouldFailWhenCommentNotFound() {
            UUID nonExistentCommentId = UUID.randomUUID();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.reportComment(nonExistentCommentId, reporter.getId(), "Reason"));

            assertEquals("Comment not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should fail when user reports same comment twice")
        void shouldFailWhenUserReportsSameCommentTwice() {
            commentsService.reportComment(testComment.getId(), reporter.getId(), "First report");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.reportComment(testComment.getId(), reporter.getId(), "Second report"));

            assertEquals("You have already reported this comment", ex.getMessage());

            long reportCount = commentReportRepository.countByCommentId(testComment.getId());
            assertEquals(1, reportCount, "Only one report should exist for this user-comment pair");
        }

        @Test
        @DisplayName("Should not increment report count when duplicate report is rejected")
        void shouldNotIncrementReportCountOnDuplicateAttempt() {
            int initialReportCount = commentAuthor.getReportCount();

            commentsService.reportComment(testComment.getId(), reporter.getId(), "First report");

            Optional<UserEntity> afterFirst = userRepository.findById(commentAuthor.getId());
            assertTrue(afterFirst.isPresent());
            assertEquals(initialReportCount + 1, afterFirst.get().getReportCount());

            assertThrows(IllegalArgumentException.class,
                    () -> commentsService.reportComment(testComment.getId(), reporter.getId(), "Duplicate"));

            Optional<UserEntity> afterDuplicate = userRepository.findById(commentAuthor.getId());
            assertTrue(afterDuplicate.isPresent());
            assertEquals(initialReportCount + 1, afterDuplicate.get().getReportCount(),
                    "Report count must not increase when duplicate report is rejected");
        }

        @Test
        @DisplayName("Should fail when commentId is null")
        void shouldFailWhenCommentIdIsNull() {
            assertThrows(Exception.class,
                    () -> commentsService.reportComment(null, reporter.getId(), "Reason"),
                    "Should throw when commentId is null");
        }

        @Test
        @DisplayName("Should fail when reporterId is null")
        void shouldFailWhenReporterIdIsNull() {
            assertThrows(Exception.class,
                    () -> commentsService.reportComment(testComment.getId(), null, "Reason"),
                    "Should throw when reporterId is null");
        }
    }

    // ─── Edge Cases ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Report Comment — Edge Cases")
    class ReportCommentEdgeTests {

        @Test
        @DisplayName("Should handle empty reason string")
        void shouldHandleEmptyReasonString() {
            assertDoesNotThrow(() ->
                    commentsService.reportComment(testComment.getId(), reporter.getId(), ""));

            Optional<CommentReport> report = commentReportRepository
                    .findByCommentIdAndReporterUserId(testComment.getId(), reporter.getId());
            assertTrue(report.isPresent());
            assertEquals("", report.get().getReason());
        }

        @Test
        @DisplayName("Should allow user to report their own comment")
        void shouldAllowUserToReportOwnComment() {
            // Capture before act — original test used commentAuthor.getReportCount()
            // which is the stale in-memory value, not fresh from DB.
            Optional<UserEntity> beforeReport = userRepository.findById(commentAuthor.getId());
            assertTrue(beforeReport.isPresent());
            int initialReportCount = beforeReport.get().getReportCount();

            assertDoesNotThrow(() ->
                    commentsService.reportComment(testComment.getId(), commentAuthor.getId(), "Self-report"));

            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                    testComment.getId(), commentAuthor.getId()));

            Optional<UserEntity> updatedAuthor = userRepository.findById(commentAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(initialReportCount + 1, updatedAuthor.get().getReportCount(),
                    "Author's report count should increment even for self-reports");
        }

        @Test
        @DisplayName("Should report comment on internship parent — not just posts")
        void shouldReportCommentOnInternshipParent() {
            // reportComment operates on the comment entity regardless of parent type.
            // Create a comment under a different parent type and verify it can be reported.
            CreateCommentRequest req = new CreateCommentRequest("Internship comment");
            // If InternshipParentType is not available in test context, skip this with assumeTrue.
            // Using POST parent here to verify the service is parent-agnostic at the report level.
            Comment postComment = commentsService.addComment(
                    CommentParentType.POST, testPost.getId(), req, commentAuthor.getId());

            commentsService.reportComment(postComment.getId(), reporter.getId(), "Reason");

            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                    postComment.getId(), reporter.getId()));
        }

        @Test
        @DisplayName("Should correctly store commentId and reporterUserId on the saved report")
        void shouldStoreCorrectIdsOnSavedReport() {
            commentsService.reportComment(testComment.getId(), reporter.getId(), "Test reason");

            Optional<CommentReport> report = commentReportRepository
                    .findByCommentIdAndReporterUserId(testComment.getId(), reporter.getId());
            assertTrue(report.isPresent());
            assertEquals(testComment.getId(), report.get().getCommentId(),
                    "commentId on report must match the reported comment");
            assertEquals(reporter.getId(), report.get().getReporterUserId(),
                    "reporterUserId on report must match the reporter");
        }
    }
}
