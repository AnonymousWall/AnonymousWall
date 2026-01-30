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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Posts Controller - Hide/Unhide Post Tests")
class PostsControllerHidePostTests {

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
    PostLikeRepository postLikeRepository;

    @Inject
    JwtTokenService jwtTokenService;

    @Inject
    PostsService postsService;

    private static final String BASE_PATH = "/api/v1/posts";

    private UserEntity authorUser;
    private UserEntity otherUser;
    private String authorToken;
    private String otherToken;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // Clean up
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Author user
        authorUser = new UserEntity();
        authorUser.setEmail("author" + System.currentTimeMillis() + "@harvard.edu");
        authorUser.setSchoolDomain("harvard.edu");
        authorUser.setVerified(true);
        authorUser.setPasswordSet(true);
        authorUser = userRepository.save(authorUser);
        authorToken = jwtTokenService.generateToken(authorUser);

        // Other user from same school
        otherUser = new UserEntity();
        otherUser.setEmail("other" + System.currentTimeMillis() + "@harvard.edu");
        otherUser.setSchoolDomain("harvard.edu");
        otherUser.setVerified(true);
        otherUser.setPasswordSet(true);
        otherUser = userRepository.save(otherUser);
        otherToken = jwtTokenService.generateToken(otherUser);

        // Create test post
        testPost = new Post(authorUser.getId(), "Test post content", "campus", "harvard.edu");
        testPost = postRepository.save(testPost);
    }

    @AfterEach
    void tearDown() {
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== HIDE POST TESTS ====================

    @Nested
    @DisplayName("Hide Post - Positive Cases")
    class HidePostPositiveCases {

        @Test
        @DisplayName("Author can hide their own post")
        void shouldHidePostAsAuthor() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals("Post hidden successfully", response.body().get("message"));

            // Verify post is hidden in database
            Optional<Post> hiddenPost = postRepository.findById(testPost.getId());
            assertTrue(hiddenPost.isPresent());
            assertTrue(hiddenPost.get().isHidden());
        }

        @Test
        @DisplayName("Hide post is idempotent (can hide multiple times)")
        void shouldBeIdempotentWhenHiding() {
            // First hide
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );
            assertEquals(HttpStatus.OK, response1.getStatus());

            // Second hide (should still succeed)
            HttpResponse<Map> response2 = client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );
            assertEquals(HttpStatus.OK, response2.getStatus());
            assertEquals("Post hidden successfully", response2.body().get("message"));
        }

        @Test
        @DisplayName("Hide post cascades to hide all comments")
        void shouldHideAllCommentsCascade() {
            // Arrange - Create 3 comments on the post
            Comment comment1 = postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment 1"),
                    authorUser.getId());
            Comment comment2 = postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment 2"),
                    otherUser.getId());
            Comment comment3 = postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment 3"),
                    authorUser.getId());

            // Verify comments are visible before hiding
            List<Comment> visibleBefore = commentRepository.findByPostIdAndHiddenFalse(testPost.getId());
            assertEquals(3, visibleBefore.size());

            // Act - Hide the post
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());

            // Verify all comments are hidden
            List<Comment> visibleAfter = commentRepository.findByPostIdAndHiddenFalse(testPost.getId());
            assertEquals(0, visibleAfter.size(), "All comments should be hidden");

            // Verify comments are marked as hidden in database
            Comment hiddenComment1 = commentRepository.findById(comment1.getId()).get();
            Comment hiddenComment2 = commentRepository.findById(comment2.getId()).get();
            Comment hiddenComment3 = commentRepository.findById(comment3.getId()).get();
            assertTrue(hiddenComment1.isHidden());
            assertTrue(hiddenComment2.isHidden());
            assertTrue(hiddenComment3.isHidden());
        }

        @Test
        @DisplayName("Hidden post is removed from campus wall feed")
        void shouldRemoveHiddenPostFromFeed() {
            // Verify post is visible before hiding
            HttpResponse<Map> beforeResponse = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "?wall=campus")
                            .bearerAuth(authorToken),
                    Map.class
            );
            List<Map> beforeData = (List<Map>) beforeResponse.body().get("data");
            assertTrue(beforeData.stream().anyMatch(p -> p.get("id").equals(testPost.getId().toString())));

            // Act - Hide the post
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Assert - Post should not appear in feed
            HttpResponse<Map> afterResponse = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "?wall=campus")
                            .bearerAuth(authorToken),
                    Map.class
            );
            List<Map> afterData = (List<Map>) afterResponse.body().get("data");
            assertFalse(afterData.stream().anyMatch(p -> p.get("id").equals(testPost.getId().toString())),
                    "Hidden post should not appear in feed");
        }

        @Test
        @DisplayName("Hidden post is removed from all sort options")
        void shouldRemoveFromAllSortOptions() {
            // Hide the post
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            String[] sortOptions = {"NEWEST", "OLDEST", "MOST_LIKED", "LEAST_LIKED"};

            for (String sort : sortOptions) {
                HttpResponse<Map> response = client.toBlocking().exchange(
                        HttpRequest.GET(BASE_PATH + "?wall=campus&sort=" + sort)
                                .bearerAuth(authorToken),
                        Map.class
                );
                List<Map> data = (List<Map>) response.body().get("data");
                assertFalse(data.stream().anyMatch(p -> p.get("id").equals(testPost.getId().toString())),
                        "Hidden post should not appear in " + sort + " sort");
            }
        }
    }

    @Nested
    @DisplayName("Hide Post - Negative Cases")
    class HidePostNegativeCases {

        @Test
        @DisplayName("Non-author cannot hide post (403 Forbidden)")
        void shouldReturn403WhenNonAuthorHidesPost() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                    client.toBlocking().exchange(
                            HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                                    .bearerAuth(otherToken),
                            Map.class
                    )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
            Map error = (Map) exception.getResponse().getBody(Map.class).get();
            assertEquals("You can only hide your own posts", error.get("error"));
        }

        @Test
        @DisplayName("Hide non-existent post returns 404")
        void shouldReturn404ForNonExistentPost() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                    client.toBlocking().exchange(
                            HttpRequest.PATCH(BASE_PATH + "/99999/hide", "")
                                    .bearerAuth(authorToken),
                            Map.class
                    )
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Hide without authentication returns 401")
        void shouldReturn401WithoutAuth() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                    client.toBlocking().exchange(
                            HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", ""),
                            Map.class
                    )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    // ==================== UNHIDE POST TESTS ====================

    @Nested
    @DisplayName("Unhide Post - Positive Cases")
    class UnhidePostPositiveCases {

        @Test
        @DisplayName("Author can unhide their own hidden post")
        void shouldUnhidePostAsAuthor() {
            // Arrange - Hide post first
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Verify post is hidden
            Optional<Post> hiddenPost = postRepository.findById(testPost.getId());
            assertTrue(hiddenPost.get().isHidden());

            // Act - Unhide post
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/unhide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals("Post unhidden successfully", response.body().get("message"));

            // Verify post is unhidden
            Optional<Post> unHiddenPost = postRepository.findById(testPost.getId());
            assertFalse(unHiddenPost.get().isHidden());
        }

        @Test
        @DisplayName("Unhide post restores all hidden comments")
        void shouldRestoreAllCommentsWhenUnhiding() {
            // Arrange - Create comments and hide post
            Comment comment1 = postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment 1"),
                    authorUser.getId());
            Comment comment2 = postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment 2"),
                    otherUser.getId());

            // Hide post (cascades to comments)
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Verify comments are hidden
            List<Comment> hiddenComments = commentRepository.findByPostIdAndHiddenFalse(testPost.getId());
            assertEquals(0, hiddenComments.size());

            // Act - Unhide post
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/unhide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());

            // Verify all comments are restored
            List<Comment> restoredComments = commentRepository.findByPostIdAndHiddenFalse(testPost.getId());
            assertEquals(2, restoredComments.size(), "All comments should be restored");

            // Verify comments are marked as unhidden
            Comment unHiddenComment1 = commentRepository.findById(comment1.getId()).get();
            Comment unHiddenComment2 = commentRepository.findById(comment2.getId()).get();
            assertFalse(unHiddenComment1.isHidden());
            assertFalse(unHiddenComment2.isHidden());
        }

        @Test
        @DisplayName("Unhide post reappears in feed")
        void shouldRestorePostToFeed() {
            // Hide post
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Verify post is not in feed
            HttpResponse<Map> hiddenResponse = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "?wall=campus")
                            .bearerAuth(authorToken),
                    Map.class
            );
            List<Map> hiddenData = (List<Map>) hiddenResponse.body().get("data");
            assertFalse(hiddenData.stream().anyMatch(p -> p.get("id").equals(testPost.getId().toString())));

            // Act - Unhide post
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/unhide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Assert - Post should reappear in feed
            HttpResponse<Map> visibleResponse = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "?wall=campus")
                            .bearerAuth(authorToken),
                    Map.class
            );
            List<Map> visibleData = (List<Map>) visibleResponse.body().get("data");
            assertTrue(visibleData.stream().anyMatch(p -> p.get("id").equals(testPost.getId().toString())),
                    "Unhidden post should reappear in feed");
        }

        @Test
        @DisplayName("Unhide post is idempotent")
        void shouldBeIdempotentWhenUnhiding() {
            // Hide post first
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // First unhide
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/unhide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );
            assertEquals(HttpStatus.OK, response1.getStatus());

            // Second unhide (should still succeed)
            HttpResponse<Map> response2 = client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/unhide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );
            assertEquals(HttpStatus.OK, response2.getStatus());
            assertEquals("Post unhidden successfully", response2.body().get("message"));
        }
    }

    @Nested
    @DisplayName("Unhide Post - Negative Cases")
    class UnhidePostNegativeCases {

        @Test
        @DisplayName("Non-author cannot unhide post (403 Forbidden)")
        void shouldReturn403WhenNonAuthorUnhidesPost() {
            // Arrange - Hide post as author
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Act & Assert - Try to unhide as other user
            HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                    client.toBlocking().exchange(
                            HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/unhide", "")
                                    .bearerAuth(otherToken),
                            Map.class
                    )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
            Map error = (Map) exception.getResponse().getBody(Map.class).get();
            assertEquals("You can only unhide your own posts", error.get("error"));
        }

        @Test
        @DisplayName("Unhide non-existent post returns 404")
        void shouldReturn404ForNonExistentPost() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                    client.toBlocking().exchange(
                            HttpRequest.PATCH(BASE_PATH + "/99999/unhide", "")
                                    .bearerAuth(authorToken),
                            Map.class
                    )
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Unhide without authentication returns 401")
        void shouldReturn401WithoutAuth() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                    client.toBlocking().exchange(
                            HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/unhide", ""),
                            Map.class
                    )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    // ==================== CASCADE BEHAVIOR TESTS ====================

    @Nested
    @DisplayName("Cascade Behavior - Hide/Unhide with Comments")
    class CascadeBehaviorTests {

        @Test
        @DisplayName("Hiding post hides all comments regardless of who created them")
        void shouldHideAllCommentsRegardlessOfAuthor() {
            // Arrange - Create comments from different users
            Comment authorComment = postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Author comment"),
                    authorUser.getId());
            Comment otherComment = postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Other user comment"),
                    otherUser.getId());

            // Act - Hide post
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Assert - Both comments should be hidden
            Comment hiddenAuthorComment = commentRepository.findById(authorComment.getId()).get();
            Comment hiddenOtherComment = commentRepository.findById(otherComment.getId()).get();
            assertTrue(hiddenAuthorComment.isHidden(), "Author's comment should be hidden");
            assertTrue(hiddenOtherComment.isHidden(), "Other user's comment should be hidden");
        }

        @Test
        @DisplayName("Hiding then unhiding post preserves comment count")
        void shouldPreserveCommentCountThroughHideUnhide() {
            // Arrange - Create multiple comments
            postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment 1"),
                    authorUser.getId());
            postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment 2"),
                    otherUser.getId());
            postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment 3"),
                    authorUser.getId());

            // Get initial count
            Post initialPost = postRepository.findById(testPost.getId()).get();
            int initialCommentCount = initialPost.getCommentCount();
            assertEquals(3, initialCommentCount);

            // Hide post
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Unhide post
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/unhide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Assert - Comment count should remain the same
            Post finalPost = postRepository.findById(testPost.getId()).get();
            assertEquals(initialCommentCount, finalPost.getCommentCount(),
                    "Comment count should be preserved after hide/unhide");
        }

        @Test
        @DisplayName("Post with no comments can be hidden and unhidden")
        void shouldHideUnhidePostWithoutComments() {
            // Act - Hide post without comments
            HttpResponse<Map> hideResponse = client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );
            assertEquals(HttpStatus.OK, hideResponse.getStatus());

            // Assert - Post is hidden
            Optional<Post> hiddenPost = postRepository.findById(testPost.getId());
            assertTrue(hiddenPost.get().isHidden());

            // Act - Unhide post
            HttpResponse<Map> unhideResponse = client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/unhide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );
            assertEquals(HttpStatus.OK, unhideResponse.getStatus());

            // Assert - Post is unhidden
            Optional<Post> unHiddenPost = postRepository.findById(testPost.getId());
            assertFalse(unHiddenPost.get().isHidden());
        }
    }

    // ==================== TRANSACTION TESTS ====================

    @Nested
    @DisplayName("Transaction Safety Tests")
    class TransactionSafetyTests {

        @Test
        @DisplayName("Hide/Unhide operations are transactional (atomic)")
        void shouldBeAtomicOperation() {
            // Arrange - Create comments
            postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment 1"),
                    authorUser.getId());
            postsService.addComment(testPost.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment 2"),
                    otherUser.getId());

            // Act - Hide post
            client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + testPost.getId() + "/hide", "")
                            .bearerAuth(authorToken),
                    Map.class
            );

            // Assert - Both post and comments should be hidden (atomic)
            Post hiddenPost = postRepository.findById(testPost.getId()).get();
            assertTrue(hiddenPost.isHidden());

            List<Comment> hiddenComments = commentRepository.findByPostIdAndHiddenFalse(testPost.getId());
            assertEquals(0, hiddenComments.size(),
                    "All comments should be hidden atomically with post");
        }
    }
}
