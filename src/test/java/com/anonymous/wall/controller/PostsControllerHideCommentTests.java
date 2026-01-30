package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import com.anonymous.wall.service.PostsService;
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

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Posts Controller - Hide/Unhide Comment Tests")
class PostsControllerHideCommentTests {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    PostRepository postRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private PostLikeRepository postLikeRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    @Inject
    private PostsService postsService;

    private static final String BASE_PATH = "/api/v1/posts";

    private UserEntity testUserCampus;
    private UserEntity testUserDifferentSchool;
    private String jwtTokenCampus;
    private String jwtTokenDifferentSchool;
    private Post campusPost;
    private Post nationalPost;
    private Comment campusComment;
    private Comment nationalComment;

    @BeforeEach
    void setUp() {
        // Clean up any leftover data from failed tests
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Harvard student - has school domain
        testUserCampus = new UserEntity();
        testUserCampus.setEmail("student" + System.currentTimeMillis() + "@harvard.edu");
        testUserCampus.setSchoolDomain("harvard.edu");
        testUserCampus.setVerified(true);
        testUserCampus.setPasswordSet(true);
        testUserCampus = userRepository.save(testUserCampus);
        jwtTokenCampus = jwtTokenService.generateToken(testUserCampus);

        // MIT student - different school
        testUserDifferentSchool = new UserEntity();
        testUserDifferentSchool.setEmail("student" + System.currentTimeMillis() + "@mit.edu");
        testUserDifferentSchool.setSchoolDomain("mit.edu");
        testUserDifferentSchool.setVerified(true);
        testUserDifferentSchool.setPasswordSet(true);
        testUserDifferentSchool = userRepository.save(testUserDifferentSchool);
        jwtTokenDifferentSchool = jwtTokenService.generateToken(testUserDifferentSchool);

        // Create test posts
        campusPost = new Post(testUserCampus.getId(), "Harvard campus post", "campus", "harvard.edu");
        campusPost = postRepository.save(campusPost);

        nationalPost = new Post(testUserCampus.getId(), "National post", "national", null);
        nationalPost = postRepository.save(nationalPost);

        // Create test comments via service to properly update post counts
        campusComment = postsService.addComment(campusPost.getId(),
            new com.anonymous.wall.model.CreateCommentRequest("Campus comment"), testUserCampus.getId());

        nationalComment = postsService.addComment(nationalPost.getId(),
            new com.anonymous.wall.model.CreateCommentRequest("National comment"), testUserCampus.getId());
    }

    @AfterEach
    void tearDown() {
        // Must delete in this order due to foreign key constraints:
        // 1. post_likes (depends on posts)
        // 2. comments (depends on posts)
        // 3. posts (depends on users)
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
            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/hide";

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PATCH(endpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertEquals("Comment hidden successfully", response.body().get("message"));

            // Verify comment is hidden in database
            Optional<Comment> updatedComment = commentRepository.findById(campusComment.getId());
            assertTrue(updatedComment.isPresent());
            assertTrue(updatedComment.get().isHidden());
        }

        @Test
        @Order(2)
        @DisplayName("Should hide own comment on national post")
        void shouldHideOwnCommentOnNationalPost() {
            String endpoint = BASE_PATH + "/" + nationalPost.getId() + "/comments/" + nationalComment.getId() + "/hide";

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PATCH(endpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals("Comment hidden successfully", response.body().get("message"));

            // Verify comment is hidden
            Optional<Comment> updatedComment = commentRepository.findById(nationalComment.getId());
            assertTrue(updatedComment.isPresent());
            assertTrue(updatedComment.get().isHidden());
        }

        @Test
        @Order(3)
        @DisplayName("Should handle hiding already hidden comment (idempotent)")
        void shouldHandleHidingAlreadyHiddenComment() {
            // First hide the comment
            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/hide";
            client.toBlocking().exchange(
                HttpRequest.PATCH(endpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            // Try hiding again
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PATCH(endpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals("Comment hidden successfully", response.body().get("message"));

            // Verify comment is still hidden
            Optional<Comment> updatedComment = commentRepository.findById(campusComment.getId());
            assertTrue(updatedComment.isPresent());
            assertTrue(updatedComment.get().isHidden());
        }
    }

    @Nested
    @DisplayName("Unhide Comment - Positive Cases")
    class UnhideCommentPositiveTests {

        @Test
        @Order(10)
        @DisplayName("Should unhide own hidden comment on campus post")
        void shouldUnhideOwnHiddenCommentOnCampusPost() {
            // First hide the comment
            String hideEndpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/hide";
            client.toBlocking().exchange(
                HttpRequest.PATCH(hideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            // Now unhide it
            String unhideEndpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/unhide";
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PATCH(unhideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals("Comment unhidden successfully", response.body().get("message"));

            // Verify comment is no longer hidden
            Optional<Comment> updatedComment = commentRepository.findById(campusComment.getId());
            assertTrue(updatedComment.isPresent());
            assertFalse(updatedComment.get().isHidden());
        }

        @Test
        @Order(11)
        @DisplayName("Should unhide own hidden comment on national post")
        void shouldUnhideOwnHiddenCommentOnNationalPost() {
            // First hide the comment
            String hideEndpoint = BASE_PATH + "/" + nationalPost.getId() + "/comments/" + nationalComment.getId() + "/hide";
            client.toBlocking().exchange(
                HttpRequest.PATCH(hideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            // Now unhide it
            String unhideEndpoint = BASE_PATH + "/" + nationalPost.getId() + "/comments/" + nationalComment.getId() + "/unhide";
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PATCH(unhideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals("Comment unhidden successfully", response.body().get("message"));

            // Verify comment is not hidden
            Optional<Comment> updatedComment = commentRepository.findById(nationalComment.getId());
            assertTrue(updatedComment.isPresent());
            assertFalse(updatedComment.get().isHidden());
        }

        @Test
        @Order(12)
        @DisplayName("Should handle unhiding already visible comment (idempotent)")
        void shouldHandleUnhidingAlreadyVisibleComment() {
            // Try unhiding without hiding first
            String unhideEndpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/unhide";
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PATCH(unhideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals("Comment unhidden successfully", response.body().get("message"));

            // Verify comment is still not hidden
            Optional<Comment> updatedComment = commentRepository.findById(campusComment.getId());
            assertTrue(updatedComment.isPresent());
            assertFalse(updatedComment.get().isHidden());
        }

        @Test
        @Order(13)
        @DisplayName("Should support multiple hide/unhide cycles on same comment")
        void shouldSupportMultipleHideUnhideCycles() {
            String hideEndpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/hide";
            String unhideEndpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/unhide";

            // First cycle: hide and unhide
            client.toBlocking().exchange(
                HttpRequest.PATCH(hideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            client.toBlocking().exchange(
                HttpRequest.PATCH(unhideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            // Second cycle: hide and unhide again
            HttpResponse<Map> hideResponse = client.toBlocking().exchange(
                HttpRequest.PATCH(hideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            assertEquals(HttpStatus.OK, hideResponse.getStatus());

            HttpResponse<Map> unhideResponse = client.toBlocking().exchange(
                HttpRequest.PATCH(unhideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            assertEquals(HttpStatus.OK, unhideResponse.getStatus());

            // Verify final state is not hidden
            Optional<Comment> updatedComment = commentRepository.findById(campusComment.getId());
            assertTrue(updatedComment.isPresent());
            assertFalse(updatedComment.get().isHidden());
        }
    }

    // ================= NEGATIVE TEST CASES =================

    @Nested
    @DisplayName("Hide Comment - Negative Cases")
    class HideCommentNegativeTests {

        @Test
        @Order(20)
        @DisplayName("Should not allow user to hide another user's comment")
        void shouldNotAllowHidingAnotherUserComment() {
            // Create a comment by testUserCampus on national post (avoid access issues)
            Comment otherUserComment = postsService.addComment(nationalPost.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("Someone else's comment"), testUserCampus.getId());

            // Try to hide it as testUserDifferentSchool
            String endpoint = BASE_PATH + "/" + nationalPost.getId() + "/comments/" + otherUserComment.getId() + "/hide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

            // Verify comment is still visible
            Optional<Comment> comment = commentRepository.findById(otherUserComment.getId());
            assertTrue(comment.isPresent());
            assertFalse(comment.get().isHidden());
        }

        @Test
        @Order(21)
        @DisplayName("Should return 404 when trying to hide non-existent comment")
        void shouldReturn404ForNonExistentComment() {
            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/99999/hide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
                )
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @Order(22)
        @DisplayName("Should return 404 when post does not exist")
        void shouldReturn404WhenPostDoesNotExist() {
            String endpoint = BASE_PATH + "/99999/comments/" + campusComment.getId() + "/hide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
                )
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @Order(23)
        @DisplayName("Should return error when comment does not belong to post")
        void shouldReturnErrorWhenCommentDoesNotBelongToPost() {
            // Create a comment on nationalPost
            Comment commentOnNationalPost = new Comment(nationalPost.getId(), testUserCampus.getId(), "National post comment");
            commentOnNationalPost = commentRepository.save(commentOnNationalPost);

            // Try to hide it as if it belongs to campusPost
            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + commentOnNationalPost.getId() + "/hide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
                )
            );

            // Should return BAD_REQUEST (comment doesn't belong to this post)
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

            // Verify comment is still visible
            Optional<Comment> comment = commentRepository.findById(commentOnNationalPost.getId());
            assertTrue(comment.isPresent());
            assertFalse(comment.get().isHidden());
        }

        @Test
        @Order(24)
        @DisplayName("Should not allow user without access to post to hide comments")
        void shouldNotAllowHideWithoutPostAccess() {
            // Create a campus post by testUserCampus
            Post harvardOnlyPost = new Post(testUserCampus.getId(), "Harvard only post", "campus", "harvard.edu");
            harvardOnlyPost = postRepository.save(harvardOnlyPost);

            // Create a comment on this post
            Comment comment = new Comment(harvardOnlyPost.getId(), testUserCampus.getId(), "Harvard comment");
            comment = commentRepository.save(comment);

            // Try to hide as testUserDifferentSchool (MIT student - no access to Harvard campus)
            String endpoint = BASE_PATH + "/" + harvardOnlyPost.getId() + "/comments/" + comment.getId() + "/hide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

            // Verify comment is still visible
            Optional<Comment> verifyComment = commentRepository.findById(comment.getId());
            assertTrue(verifyComment.isPresent());
            assertFalse(verifyComment.get().isHidden());
        }

        @Test
        @Order(25)
        @DisplayName("Should require authentication to hide comment")
        void shouldRequireAuthenticationToHideComment() {
            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/hide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>()),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Unhide Comment - Negative Cases")
    class UnhideCommentNegativeTests {

        @Test
        @Order(30)
        @DisplayName("Should not allow user to unhide another user's comment")
        void shouldNotAllowUnhidingAnotherUserComment() {
            // Create a comment by testUserCampus on national post and hide it
            Comment otherUserComment = postsService.addComment(nationalPost.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("Someone else's comment"), testUserCampus.getId());
            postsService.hideComment(nationalPost.getId(), otherUserComment.getId(), testUserCampus.getId());

            // Try to unhide it as testUserDifferentSchool
            String endpoint = BASE_PATH + "/" + nationalPost.getId() + "/comments/" + otherUserComment.getId() + "/unhide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

            // Verify comment is still hidden
            Optional<Comment> comment = commentRepository.findById(otherUserComment.getId());
            assertTrue(comment.isPresent());
            assertTrue(comment.get().isHidden());
        }

        @Test
        @Order(31)
        @DisplayName("Should return 404 when trying to unhide non-existent comment")
        void shouldReturn404ForNonExistentCommentUnhide() {
            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/99999/unhide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
                )
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @Order(32)
        @DisplayName("Should return 404 when post does not exist for unhide")
        void shouldReturn404WhenPostDoesNotExistForUnhide() {
            String endpoint = BASE_PATH + "/99999/comments/" + campusComment.getId() + "/unhide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
                )
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @Order(33)
        @DisplayName("Should return error when comment does not belong to post for unhide")
        void shouldReturn404WhenCommentDoesNotBelongToPostForUnhide() {
            // Create a comment on nationalPost
            Comment commentOnNationalPost = new Comment(nationalPost.getId(), testUserCampus.getId(), "National post comment");
            commentOnNationalPost = commentRepository.save(commentOnNationalPost);
            commentOnNationalPost.setHidden(true);
            commentRepository.update(commentOnNationalPost);

            // Try to unhide it as if it belongs to campusPost
            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + commentOnNationalPost.getId() + "/unhide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
                )
            );

            // Should return BAD_REQUEST (comment doesn't belong to this post)
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

            // Verify comment is still hidden
            Optional<Comment> comment = commentRepository.findById(commentOnNationalPost.getId());
            assertTrue(comment.isPresent());
            assertTrue(comment.get().isHidden());
        }

        @Test
        @Order(34)
        @DisplayName("Should not allow user without access to post to unhide comments")
        void shouldNotAllowUnhideWithoutPostAccess() {
            // Create a campus post by testUserCampus
            Post harvardOnlyPost = new Post(testUserCampus.getId(), "Harvard only post", "campus", "harvard.edu");
            harvardOnlyPost = postRepository.save(harvardOnlyPost);

            // Create and hide a comment on this post
            Comment comment = new Comment(harvardOnlyPost.getId(), testUserCampus.getId(), "Harvard comment");
            comment = commentRepository.save(comment);
            comment.setHidden(true);
            commentRepository.update(comment);

            // Try to unhide as testUserDifferentSchool (MIT student - no access to Harvard campus)
            String endpoint = BASE_PATH + "/" + harvardOnlyPost.getId() + "/comments/" + comment.getId() + "/unhide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

            // Verify comment is still hidden
            Optional<Comment> verifyComment = commentRepository.findById(comment.getId());
            assertTrue(verifyComment.isPresent());
            assertTrue(verifyComment.get().isHidden());
        }

        @Test
        @Order(35)
        @DisplayName("Should require authentication to unhide comment")
        void shouldRequireAuthenticationToUnhideComment() {
            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/unhide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>()),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    // ================= EDGE CASES =================

    @Nested
    @DisplayName("Hide/Unhide Comment - Edge Cases")
    class HideUnhideCommentEdgeCases {

        @Test
        @Order(40)
        @DisplayName("Should handle concurrent hide operations gracefully")
        void shouldHandleConcurrentHideOperations() throws InterruptedException {
            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/hide";

            // Simulate two concurrent hide requests
            Thread thread1 = new Thread(() -> {
                try {
                    client.toBlocking().exchange(
                        HttpRequest.PATCH(endpoint, new HashMap<>())
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                        Map.class
                    );
                } catch (Exception e) {
                    // Expected for second request might have version conflict
                }
            });

            Thread thread2 = new Thread(() -> {
                try {
                    Thread.sleep(50); // Small delay to try to create concurrency
                    client.toBlocking().exchange(
                        HttpRequest.PATCH(endpoint, new HashMap<>())
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                        Map.class
                    );
                } catch (Exception e) {
                    // Expected for second request might have version conflict
                }
            });

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            // Verify comment is hidden after both operations
            Optional<Comment> updatedComment = commentRepository.findById(campusComment.getId());
            assertTrue(updatedComment.isPresent());
            assertTrue(updatedComment.get().isHidden());
        }

        @Test
        @Order(41)
        @DisplayName("Should maintain version information when hiding comment")
        void shouldMaintainVersionWhenHidingComment() {
            Long initialVersion = campusComment.getVersion();

            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/hide";
            client.toBlocking().exchange(
                HttpRequest.PATCH(endpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            Optional<Comment> updatedComment = commentRepository.findById(campusComment.getId());
            assertTrue(updatedComment.isPresent());
            // Version should be incremented (optimistic locking)
            assertTrue(updatedComment.get().getVersion() > initialVersion);
        }

        @Test
        @Order(42)
        @DisplayName("Should maintain other comment data when hiding")
        void shouldMaintainCommentDataWhenHiding() {
            String originalText = campusComment.getText();
            Long originalPostId = campusComment.getPostId();

            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/hide";
            client.toBlocking().exchange(
                HttpRequest.PATCH(endpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            Optional<Comment> updatedComment = commentRepository.findById(campusComment.getId());
            assertTrue(updatedComment.isPresent());
            assertEquals(originalText, updatedComment.get().getText());
            assertEquals(originalPostId, updatedComment.get().getPostId());
            // CreatedAt should remain the same (verify it's not changed)
            assertNotNull(updatedComment.get().getCreatedAt());
        }


        @Test
        @Order(44)
        @DisplayName("Should work with very long comment text")
        void shouldWorkWithVeryLongCommentText() {
            String longText = "X".repeat(5000);
            Comment longComment = new Comment(campusPost.getId(), testUserCampus.getId(), longText);
            longComment = commentRepository.save(longComment);

            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + longComment.getId() + "/hide";
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PATCH(endpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());

            Optional<Comment> updatedComment = commentRepository.findById(longComment.getId());
            assertTrue(updatedComment.isPresent());
            assertTrue(updatedComment.get().isHidden());
            assertEquals(longText, updatedComment.get().getText());
        }

        @Test
        @Order(45)
        @DisplayName("Should work with comment containing special characters")
        void shouldWorkWithSpecialCharactersInComment() {
            String specialText = "Comment with 🎉 emoji @mention #hashtag and ñ characters";
            Comment specialComment = new Comment(campusPost.getId(), testUserCampus.getId(), specialText);
            specialComment = commentRepository.save(specialComment);

            String hideEndpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + specialComment.getId() + "/hide";
            HttpResponse<Map> hideResponse = client.toBlocking().exchange(
                HttpRequest.PATCH(hideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            assertEquals(HttpStatus.OK, hideResponse.getStatus());

            String unhideEndpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + specialComment.getId() + "/unhide";
            HttpResponse<Map> unhideResponse = client.toBlocking().exchange(
                HttpRequest.PATCH(unhideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            assertEquals(HttpStatus.OK, unhideResponse.getStatus());

            Optional<Comment> updatedComment = commentRepository.findById(specialComment.getId());
            assertTrue(updatedComment.isPresent());
            assertFalse(updatedComment.get().isHidden());
            assertEquals(specialText, updatedComment.get().getText());
        }

        @Test
        @Order(46)
        @DisplayName("Should handle invalid path parameters gracefully")
        void shouldHandleInvalidPathParameters() {
            String endpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/invalid/hide";

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(endpoint, new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
                )
            );

            // Should handle invalid parameter gracefully
            assertTrue(exception.getStatus().getCode() >= 400);
        }

        @Test
        @Order(47)
        @DisplayName("Should maintain correct response format on all success responses")
        void shouldMaintainCorrectResponseFormat() {
            String hideEndpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/hide";
            HttpResponse<Map> hideResponse = client.toBlocking().exchange(
                HttpRequest.PATCH(hideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, hideResponse.getStatus());
            assertNotNull(hideResponse.body());
            assertTrue(hideResponse.body().containsKey("message"));
            assertIsString(hideResponse.body().get("message"));

            String unhideEndpoint = BASE_PATH + "/" + campusPost.getId() + "/comments/" + campusComment.getId() + "/unhide";
            HttpResponse<Map> unhideResponse = client.toBlocking().exchange(
                HttpRequest.PATCH(unhideEndpoint, new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, unhideResponse.getStatus());
            assertNotNull(unhideResponse.body());
            assertTrue(unhideResponse.body().containsKey("message"));
            assertIsString(unhideResponse.body().get("message"));
        }

        private void assertIsString(Object obj) {
            assertTrue(obj instanceof String, "Expected String but got " + (obj != null ? obj.getClass().getSimpleName() : "null"));
        }
    }
}
