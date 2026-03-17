package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.PostReport;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.PostReportRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.PostsService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PostsService - Report Post Tests")
class PostsServiceReportPostTests {

    @Inject
    private PostsService postsService;

    @Inject
    private PostRepository postRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private PostReportRepository postReportRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private PostLikeRepository postLikeRepository;

    private UserEntity postAuthor;
    private UserEntity reporter;
    private UserEntity anotherReporter;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // Clean up in correct order
        postReportRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create post author
        postAuthor = new UserEntity();
        postAuthor.setEmail("author" + System.currentTimeMillis() + "@harvard.edu");
        postAuthor.setSchoolDomain("harvard.edu");
        postAuthor.setVerified(true);
        postAuthor.setPasswordSet(true);
        postAuthor = userRepository.save(postAuthor);

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
        testPost = new Post(postAuthor.getId(), "Test Title", "Test post content", "campus", "harvard.edu");
        testPost = postRepository.save(testPost);
    }

    @AfterEach
    void tearDown() {
        postReportRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== POSITIVE TEST CASES ====================

    @Nested
    @DisplayName("Report Post - Positive Cases")
    class ReportPostPositiveTests {

        @Test
        @Order(1)
        @DisplayName("Should report post successfully without reason")
        void shouldReportPostWithoutReason() {
            // Arrange
            int initialReportCount = postAuthor.getReportCount();

            // Act
            postsService.reportPost(testPost.getId(), reporter.getId(), null);

            // Assert
            // Verify report was created
            boolean reportExists = postReportRepository.existsByPostIdAndReporterUserId(
                testPost.getId(), reporter.getId());
            assertTrue(reportExists, "Report should exist in database");

            // Verify author's report count was incremented
            Optional<UserEntity> updatedAuthor = userRepository.findById(postAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(initialReportCount + 1, updatedAuthor.get().getReportCount(),
                "Author's report count should be incremented");
        }

        @Test
        @Order(2)
        @DisplayName("Should report post successfully with reason")
        void shouldReportPostWithReason() {
            // Arrange
            String reason = "This post contains inappropriate content";
            int initialReportCount = postAuthor.getReportCount();

            // Act
            postsService.reportPost(testPost.getId(), reporter.getId(), reason);

            // Assert
            // Verify report was created with reason
            Optional<PostReport> report = postReportRepository.findByPostIdAndReporterUserId(
                testPost.getId(), reporter.getId());
            assertTrue(report.isPresent(), "Report should exist");
            assertEquals(reason, report.get().getReason(), "Report reason should match");
            assertEquals(postAuthor.getId(), report.get().getReportedUserId(), 
                "Report should have the correct reported user ID");

            // Verify author's report count was incremented
            Optional<UserEntity> updatedAuthor = userRepository.findById(postAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(initialReportCount + 1, updatedAuthor.get().getReportCount());
        }

        @Test
        @Order(3)
        @DisplayName("Should allow different users to report same post")
        void shouldAllowDifferentUsersToReportSamePost() {
            // Arrange
            int initialReportCount = postAuthor.getReportCount();

            // Act - First reporter reports the post
            postsService.reportPost(testPost.getId(), reporter.getId(), "Reason 1");

            // Act - Second reporter reports the same post
            postsService.reportPost(testPost.getId(), anotherReporter.getId(), "Reason 2");

            // Assert
            // Verify both reports exist
            assertTrue(postReportRepository.existsByPostIdAndReporterUserId(
                testPost.getId(), reporter.getId()));
            assertTrue(postReportRepository.existsByPostIdAndReporterUserId(
                testPost.getId(), anotherReporter.getId()));

            // Verify author's report count was incremented twice
            Optional<UserEntity> updatedAuthor = userRepository.findById(postAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(initialReportCount + 2, updatedAuthor.get().getReportCount(),
                "Author's report count should be incremented by 2");
        }

        @Test
        @Order(4)
        @DisplayName("Should handle long reason text (max 500 chars)")
        void shouldHandleLongReasonText() {
            // Arrange
            String longReason = "A".repeat(500); // Max length

            // Act & Assert - Should not throw exception
            assertDoesNotThrow(() -> postsService.reportPost(testPost.getId(), reporter.getId(), longReason));

            // Verify report was created
            Optional<PostReport> report = postReportRepository.findByPostIdAndReporterUserId(
                testPost.getId(), reporter.getId());
            assertTrue(report.isPresent());
            assertEquals(longReason, report.get().getReason());
        }
    }

    // ==================== NEGATIVE TEST CASES ====================

    @Nested
    @DisplayName("Report Post - Negative Cases")
    class ReportPostNegativeTests {

        @Test
        @Order(5)
        @DisplayName("Should fail when reporting non-existent post")
        void shouldFailWhenReportingNonExistentPost() {
            // Arrange
            UUID nonExistentPostId = UUID.randomUUID();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.reportPost(nonExistentPostId, reporter.getId(), "Reason")
            );

            assertEquals("Post not found", exception.getMessage());
        }

        @Test
        @Order(6)
        @DisplayName("Should fail when user reports same post twice")
        void shouldFailWhenUserReportsSamePostTwice() {
            // Arrange - First report
            postsService.reportPost(testPost.getId(), reporter.getId(), "First report");

            // Act & Assert - Second report should fail
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.reportPost(testPost.getId(), reporter.getId(), "Second report")
            );

            assertEquals("You have already reported this post", exception.getMessage());

            // Verify only one report exists
            long reportCount = postReportRepository.countByPostId(testPost.getId());
            assertEquals(1, reportCount, "Should only have one report");
        }

        @Test
        @Order(7)
        @DisplayName("Should not double-increment report count on duplicate attempt")
        void shouldNotDoubleIncrementReportCountOnDuplicateAttempt() {
            // Arrange
            int initialReportCount = postAuthor.getReportCount();
            
            // First report
            postsService.reportPost(testPost.getId(), reporter.getId(), "First report");

            // Verify count increased by 1
            Optional<UserEntity> authorAfterFirstReport = userRepository.findById(postAuthor.getId());
            assertTrue(authorAfterFirstReport.isPresent());
            assertEquals(initialReportCount + 1, authorAfterFirstReport.get().getReportCount());

            // Act - Try to report again (should fail)
            assertThrows(IllegalArgumentException.class,
                () -> postsService.reportPost(testPost.getId(), reporter.getId(), "Second report"));

            // Assert - Report count should still be +1, not +2
            Optional<UserEntity> authorAfterDuplicate = userRepository.findById(postAuthor.getId());
            assertTrue(authorAfterDuplicate.isPresent());
            assertEquals(initialReportCount + 1, authorAfterDuplicate.get().getReportCount(),
                "Report count should not increase on duplicate report attempt");
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Report Post - Edge Cases")
    class ReportPostEdgeTests {

        @Test
        @Order(8)
        @DisplayName("Should handle empty reason string")
        void shouldHandleEmptyReasonString() {
            // Act & Assert
            assertDoesNotThrow(() -> postsService.reportPost(testPost.getId(), reporter.getId(), ""));

            // Verify report was created with empty reason
            Optional<PostReport> report = postReportRepository.findByPostIdAndReporterUserId(
                testPost.getId(), reporter.getId());
            assertTrue(report.isPresent());
            assertEquals("", report.get().getReason());
        }

        @Test
        @Order(9)
        @DisplayName("Should allow user to report their own post")
        void shouldAllowUserToReportOwnPost() {
            // Act & Assert - User can report their own post
            assertDoesNotThrow(() -> postsService.reportPost(testPost.getId(), postAuthor.getId(), "Self-report"));

            // Verify report exists
            boolean reportExists = postReportRepository.existsByPostIdAndReporterUserId(
                testPost.getId(), postAuthor.getId());
            assertTrue(reportExists);

            // Verify author's own report count was incremented
            Optional<UserEntity> updatedAuthor = userRepository.findById(postAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertTrue(updatedAuthor.get().getReportCount() > 0);
        }
    }
}
