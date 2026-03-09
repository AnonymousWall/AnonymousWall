package com.anonymous.wall.controller;
import com.anonymous.wall.model.CommentParentType;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.repository.CommentReportRepository;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostReportRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.CommentsService;
import com.anonymous.wall.service.JwtTokenService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Posts Controller - Report Tests")
class PostsControllerReportTests {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    PostRepository postRepository;

    @Inject
    CommentRepository commentRepository;

    @Inject
    PostReportRepository postReportRepository;

    @Inject
    CommentReportRepository commentReportRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    @Inject
    private CommentsService commentsService;

    private static final String POSTS_BASE_PATH = "/api/v1/posts";

    private UserEntity postAuthor;
    private UserEntity reporter;
    private UserEntity anotherReporter;
    private String jwtTokenAuthor;
    private String jwtTokenReporter;
    private String jwtTokenAnotherReporter;
    private Post testPost;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        // Clean up in correct order
        commentReportRepository.deleteAll();
        postReportRepository.deleteAll();
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
        jwtTokenAuthor = jwtTokenService.generateToken(postAuthor);

        // Create reporter user
        reporter = new UserEntity();
        reporter.setEmail("reporter" + System.currentTimeMillis() + "@harvard.edu");
        reporter.setSchoolDomain("harvard.edu");
        reporter.setVerified(true);
        reporter.setPasswordSet(true);
        reporter = userRepository.save(reporter);
        jwtTokenReporter = jwtTokenService.generateToken(reporter);

        // Create another reporter user
        anotherReporter = new UserEntity();
        anotherReporter.setEmail("another" + System.currentTimeMillis() + "@harvard.edu");
        anotherReporter.setSchoolDomain("harvard.edu");
        anotherReporter.setVerified(true);
        anotherReporter.setPasswordSet(true);
        anotherReporter = userRepository.save(anotherReporter);
        jwtTokenAnotherReporter = jwtTokenService.generateToken(anotherReporter);

        // Create test post
        testPost = new Post(postAuthor.getId(), "Test Title", "Test post content", "campus", "harvard.edu");
        testPost = postRepository.save(testPost);

        // Create test comment
        CreateCommentRequest commentRequest = new CreateCommentRequest("Test comment text");
        testComment = commentsService.addComment(CommentParentType.POST, testPost.getId(), commentRequest, postAuthor.getId());
    }

    @AfterEach
    void tearDown() {
        commentReportRepository.deleteAll();
        postReportRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== POST REPORT TESTS ====================

    @Nested
    @DisplayName("Report Post - Positive Cases")
    class ReportPostPositiveTests {

        @Test
        @Order(1)
        @DisplayName("Should report post successfully without reason")
        void shouldReportPostWithoutReason() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            // No reason provided

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenReporter),
                Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertNotNull(response.body());
            assertEquals("Post reported successfully", response.body().get("message"));

            // Verify report was created
            boolean reportExists = postReportRepository.existsByPostIdAndReporterUserId(
                testPost.getId(), reporter.getId());
            assertTrue(reportExists);

            // Verify author's report count was incremented
            Optional<UserEntity> updatedAuthor = userRepository.findById(postAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(1, updatedAuthor.get().getReportCount());
        }

        @Test
        @Order(2)
        @DisplayName("Should report post successfully with reason")
        void shouldReportPostWithReason() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "This post contains inappropriate content");

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenReporter),
                Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("Post reported successfully", response.body().get("message"));

            // Verify report exists
            boolean reportExists = postReportRepository.existsByPostIdAndReporterUserId(
                testPost.getId(), reporter.getId());
            assertTrue(reportExists);
        }

        @Test
        @Order(3)
        @DisplayName("Should allow different users to report same post")
        void shouldAllowDifferentUsersToReportSamePost() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "Inappropriate");

            // First reporter
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenReporter),
                Map.class
            );
            assertEquals(HttpStatus.CREATED, response1.getStatus());

            // Second reporter
            HttpResponse<Map> response2 = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenAnotherReporter),
                Map.class
            );
            assertEquals(HttpStatus.CREATED, response2.getStatus());

            // Verify both reports exist
            assertTrue(postReportRepository.existsByPostIdAndReporterUserId(
                testPost.getId(), reporter.getId()));
            assertTrue(postReportRepository.existsByPostIdAndReporterUserId(
                testPost.getId(), anotherReporter.getId()));

            // Verify author's report count was incremented twice
            Optional<UserEntity> updatedAuthor = userRepository.findById(postAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(2, updatedAuthor.get().getReportCount());
        }
    }

    @Nested
    @DisplayName("Report Post - Negative Cases")
    class ReportPostNegativeTests {

        @Test
        @Order(4)
        @DisplayName("Should fail when reporting non-existent post")
        void shouldFailWhenReportingNonExistentPost() {
            UUID nonExistentPostId = UUID.randomUUID();
            String reportPath = POSTS_BASE_PATH + "/" + nonExistentPostId + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "Test reason");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(reportPath, request)
                        .header("Authorization", "Bearer " + jwtTokenReporter),
                    Map.class
                )
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @Order(5)
        @DisplayName("Should fail when user reports same post twice")
        void shouldFailWhenUserReportsSamePostTwice() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "First report");

            // First report - should succeed
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenReporter),
                Map.class
            );
            assertEquals(HttpStatus.CREATED, response1.getStatus());

            // Second report - should fail
            request.put("reason", "Second report");
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(reportPath, request)
                        .header("Authorization", "Bearer " + jwtTokenReporter),
                    Map.class
                )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @Order(6)
        @DisplayName("Should fail when unauthorized")
        void shouldFailWhenUnauthorized() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "Test reason");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(reportPath, request),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    // ==================== COMMENT REPORT TESTS ====================

    @Nested
    @DisplayName("Report Comment - Positive Cases")
    class ReportCommentPositiveTests {

        @Test
        @Order(7)
        @DisplayName("Should report comment successfully without reason")
        void shouldReportCommentWithoutReason() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/comments/" + testComment.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            // No reason provided

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenReporter),
                Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertNotNull(response.body());
            assertEquals("Comment reported successfully", response.body().get("message"));

            // Verify report was created
            boolean reportExists = commentReportRepository.existsByCommentIdAndReporterUserId(
                testComment.getId(), reporter.getId());
            assertTrue(reportExists);

            // Verify author's report count was incremented
            Optional<UserEntity> updatedAuthor = userRepository.findById(postAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(1, updatedAuthor.get().getReportCount());
        }

        @Test
        @Order(8)
        @DisplayName("Should report comment successfully with reason")
        void shouldReportCommentWithReason() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/comments/" + testComment.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "This comment is offensive");

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenReporter),
                Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("Comment reported successfully", response.body().get("message"));

            // Verify report exists
            boolean reportExists = commentReportRepository.existsByCommentIdAndReporterUserId(
                testComment.getId(), reporter.getId());
            assertTrue(reportExists);
        }

        @Test
        @Order(9)
        @DisplayName("Should allow different users to report same comment")
        void shouldAllowDifferentUsersToReportSameComment() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/comments/" + testComment.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "Inappropriate");

            // First reporter
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenReporter),
                Map.class
            );
            assertEquals(HttpStatus.CREATED, response1.getStatus());

            // Second reporter
            HttpResponse<Map> response2 = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenAnotherReporter),
                Map.class
            );
            assertEquals(HttpStatus.CREATED, response2.getStatus());

            // Verify both reports exist
            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                testComment.getId(), reporter.getId()));
            assertTrue(commentReportRepository.existsByCommentIdAndReporterUserId(
                testComment.getId(), anotherReporter.getId()));

            // Verify author's report count was incremented twice
            Optional<UserEntity> updatedAuthor = userRepository.findById(postAuthor.getId());
            assertTrue(updatedAuthor.isPresent());
            assertEquals(2, updatedAuthor.get().getReportCount());
        }
    }

    @Nested
    @DisplayName("Report Comment - Negative Cases")
    class ReportCommentNegativeTests {

        @Test
        @Order(10)
        @DisplayName("Should fail when reporting non-existent comment")
        void shouldFailWhenReportingNonExistentComment() {
            UUID nonExistentCommentId = UUID.randomUUID();
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/comments/" + nonExistentCommentId + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "Test reason");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(reportPath, request)
                        .header("Authorization", "Bearer " + jwtTokenReporter),
                    Map.class
                )
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @Order(11)
        @DisplayName("Should fail when user reports same comment twice")
        void shouldFailWhenUserReportsSameCommentTwice() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/comments/" + testComment.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "First report");

            // First report - should succeed
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenReporter),
                Map.class
            );
            assertEquals(HttpStatus.CREATED, response1.getStatus());

            // Second report - should fail
            request.put("reason", "Second report");
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(reportPath, request)
                        .header("Authorization", "Bearer " + jwtTokenReporter),
                    Map.class
                )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @Order(12)
        @DisplayName("Should fail when unauthorized")
        void shouldFailWhenUnauthorized() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/comments/" + testComment.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "Test reason");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(reportPath, request),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Report - Edge Cases")
    class ReportEdgeCaseTests {

        @Test
        @Order(13)
        @DisplayName("Should handle empty reason string for post")
        void shouldHandleEmptyReasonStringForPost() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "");

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenReporter),
                Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
        }

        @Test
        @Order(14)
        @DisplayName("Should handle empty reason string for comment")
        void shouldHandleEmptyReasonStringForComment() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/comments/" + testComment.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "");

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenReporter),
                Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
        }

        @Test
        @Order(15)
        @DisplayName("Should allow author to report their own post")
        void shouldAllowAuthorToReportOwnPost() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "Self-report");

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenAuthor),
                Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            
            // Verify report exists
            boolean reportExists = postReportRepository.existsByPostIdAndReporterUserId(
                testPost.getId(), postAuthor.getId());
            assertTrue(reportExists);
        }

        @Test
        @Order(16)
        @DisplayName("Should allow author to report their own comment")
        void shouldAllowAuthorToReportOwnComment() {
            String reportPath = POSTS_BASE_PATH + "/" + testPost.getId() + "/comments/" + testComment.getId() + "/reports";
            Map<String, Object> request = new HashMap<>();
            request.put("reason", "Self-report");

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(reportPath, request)
                    .header("Authorization", "Bearer " + jwtTokenAuthor),
                Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            
            // Verify report exists
            boolean reportExists = commentReportRepository.existsByCommentIdAndReporterUserId(
                testComment.getId(), postAuthor.getId());
            assertTrue(reportExists);
        }
    }
}
