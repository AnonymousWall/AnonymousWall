package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CommentDTO;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
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
@DisplayName("Posts Controller - Comment Tests")
class PostsControllerCommentTests {

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

    private static final String BASE_PATH = "/api/v1/posts";

    private UserEntity testUserCampus;
    private UserEntity testUserDifferentSchool;
    private UserEntity testUserNoSchool;
    private String jwtTokenCampus;
    private String jwtTokenDifferentSchool;
    private String jwtTokenNoSchool;
    private Post campusPost;
    private Post nationalPost;

    @BeforeEach
    void setUp() {
        // Clean up any leftover data from failed tests
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();

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

        // Non-student - no school domain
        testUserNoSchool = new UserEntity();
        testUserNoSchool.setEmail("user" + System.currentTimeMillis() + "@gmail.com");
        testUserNoSchool.setSchoolDomain(null);
        testUserNoSchool.setVerified(true);
        testUserNoSchool.setPasswordSet(true);
        testUserNoSchool = userRepository.save(testUserNoSchool);
        jwtTokenNoSchool = jwtTokenService.generateToken(testUserNoSchool);

        // Create test posts
        campusPost = new Post(testUserCampus.getId(), "Harvard campus post", "campus", "harvard.edu");
        campusPost = postRepository.save(campusPost);

        nationalPost = new Post(testUserCampus.getId(), "National post", "national", null);
        nationalPost = postRepository.save(nationalPost);
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
    }

    @Nested
    @DisplayName("Add Comment - Positive Cases")
    class AddCommentPositiveTests {

        @Test
        @DisplayName("Should add valid comment to campus post from same school")
        void shouldAddCommentToCampusPost() {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "Great post!");

            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                CommentDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            CommentDTO body = response.body();
            assertNotNull(body);
            assertEquals("Great post!", body.getText());
            assertNotNull(body.getId());
            assertNotNull(body.getCreatedAt());
        }

        @Test
        @DisplayName("Should add valid comment to national post")
        void shouldAddCommentToNationalPost() {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "Nice national post!");

            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + nationalPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                CommentDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("Nice national post!", response.body().getText());
        }

        @Test
        @DisplayName("Should add comment with maximum length (5000)")
        void shouldAddMaxLengthComment() {
            String maxContent = "X".repeat(5000);
            Map<String, Object> request = new HashMap<>();
            request.put("text", maxContent);

            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                CommentDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals(5000, response.body().getText().length());
        }

        @Test
        @DisplayName("Should add comment with minimum length (1)")
        void shouldAddMinimumLengthComment() {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "A");

            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                CommentDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("A", response.body().getText());
        }

        @Test
        @DisplayName("Should add comment with special characters")
        void shouldAddCommentWithSpecialCharacters() {
            String content = "Great post! 🎉 @mention #hashtag";
            Map<String, Object> request = new HashMap<>();
            request.put("text", content);

            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                CommentDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals(content, response.body().getText());
        }
    }

    @Nested
    @DisplayName("Add Comment - Negative Cases")
    class AddCommentNegativeTests {

        @Test
        @DisplayName("Should reject empty comment")
        void shouldRejectEmptyComment() {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request)
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    CommentDTO.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should reject whitespace-only comment")
        void shouldRejectWhitespaceComment() {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "   \n\t   ");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request)
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    CommentDTO.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should reject comment exceeding 5000 characters")
        void shouldRejectTooLongComment() {
            String tooLong = "X".repeat(5001);
            Map<String, Object> request = new HashMap<>();
            request.put("text", tooLong);

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request)
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    CommentDTO.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should reject comment on non-existent post")
        void shouldRejectCommentOnMissingPost() {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "Comment on missing post");
            UUID nonExistentPostId = UUID.randomUUID();
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + nonExistentPostId + "/comments", request)
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    CommentDTO.class
                )
            );
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Should reject comment without authentication")
        void shouldRejectUnauthenticatedComment() {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "Unauthenticated comment");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request),
                    CommentDTO.class
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Add Comment - Permission Tests")
    class AddCommentPermissionTests {

        @Test
        @DisplayName("Should deny comment from different school on campus post")
        void shouldDenyCommentFromDifferentSchool() {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "MIT student trying to comment");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request)
                        .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                    CommentDTO.class
                )
            );
            assertTrue(
                exception.getStatus().equals(HttpStatus.FORBIDDEN) ||
                exception.getStatus().equals(HttpStatus.BAD_REQUEST),
                "Expected FORBIDDEN or BAD_REQUEST for cross-school comment"
            );
        }

        @Test
        @DisplayName("Should allow comment on national post from different school")
        void shouldAllowCommentOnNationalPostDifferentSchool() {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "MIT comment on national post");

            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + nationalPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                CommentDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("MIT comment on national post", response.body().getText());
        }

        @Test
        @DisplayName("Should deny comment from user without school domain on campus post")
        void shouldDenyCommentFromNoSchoolUserOnCampus() {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "Comment from non-student");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request)
                        .header("Authorization", "Bearer " + jwtTokenNoSchool),
                    CommentDTO.class
                )
            );
            assertTrue(
                exception.getStatus().equals(HttpStatus.FORBIDDEN) ||
                exception.getStatus().equals(HttpStatus.BAD_REQUEST),
                "Expected FORBIDDEN or BAD_REQUEST for user without school"
            );
        }

        @Test
        @DisplayName("Should allow comment on national post from user without school")
        void shouldAllowCommentOnNationalPostNoSchool() {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "Comment from non-student on national");

            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + nationalPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + jwtTokenNoSchool),
                CommentDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
        }
    }

    @Nested
    @DisplayName("Get Comments - Tests")
    class GetCommentsTests {

        @Test
        @DisplayName("Should retrieve comments for post")
        void shouldGetComments() {
            // Add 3 comments
            Map<String, Object> comment1 = new HashMap<>();
            comment1.put("text", "First comment");
            Map<String, Object> comment2 = new HashMap<>();
            comment2.put("text", "Second comment");
            Map<String, Object> comment3 = new HashMap<>();
            comment3.put("text", "Third comment");

            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", comment1)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                CommentDTO.class
            );
            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", comment2)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                CommentDTO.class
            );
            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", comment3)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                CommentDTO.class
            );

            // Get comments
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + campusPost.getId() + "/comments")
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> body = response.body();
            assertNotNull(body);
            assertNotNull(body.get("data"));
        }

        @Test
        @DisplayName("Should deny get comments from different school on campus post")
        void shouldDenyGetCommentsFromDifferentSchool() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + campusPost.getId() + "/comments")
                        .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                    Map.class
                )
            );
            assertTrue(
                exception.getStatus().equals(HttpStatus.FORBIDDEN) ||
                exception.getStatus().equals(HttpStatus.NOT_FOUND),
                "Expected FORBIDDEN or NOT_FOUND for cross-school access"
            );
        }

        @Test
        @DisplayName("Should allow get comments on national post from any user")
        void shouldGetCommentsOnNationalPost() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + nationalPost.getId() + "/comments")
                    .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
        }
    }

    @Nested
    @DisplayName("Comment Count - Data Integrity")
    class CommentCountTests {

        @Test
        @DisplayName("Comment count should increment when comment is added")
        void shouldIncrementCommentCount() {
            // Check initial count
            int initialCount = campusPost.getCommentCount();
            assertEquals(0, initialCount);

            // Add comment
            Map<String, Object> request = new HashMap<>();
            request.put("text", "Test comment");

            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                CommentDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());

            // Verify count increased
            Optional<Post> updatedPost = postRepository.findById(campusPost.getId());
            assertTrue(updatedPost.isPresent());
            assertTrue(updatedPost.get().getCommentCount() >= 1);
        }

        @Test
        @DisplayName("Multiple comments should all be counted correctly")
        void shouldCountMultipleComments() {
            // Add 5 comments
            for (int i = 1; i <= 5; i++) {
                Map<String, Object> request = new HashMap<>();
                request.put("text", "Comment " + i);

                client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/comments", request)
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    CommentDTO.class
                );
            }

            // Verify count
            Optional<Post> updatedPost = postRepository.findById(campusPost.getId());
            assertTrue(updatedPost.isPresent());
            assertTrue(updatedPost.get().getCommentCount() >= 5);
        }
    }

    @Nested
    @DisplayName("Comment Profile Name Tests")
    class CommentProfileNameTests {

        private UserEntity userWithDefaultName;
        private UserEntity userWithCustomName;
        private String tokenDefaultName;
        private String tokenCustomName;
        private Post testPost;

        @BeforeEach
        void setUp() {
            // User with default profile name
            userWithDefaultName = new UserEntity();
            userWithDefaultName.setEmail("comment_default" + System.currentTimeMillis() + "@harvard.edu");
            userWithDefaultName.setSchoolDomain("harvard.edu");
            userWithDefaultName.setProfileName("Anonymous");
            userWithDefaultName.setVerified(true);
            userWithDefaultName.setPasswordSet(true);
            userWithDefaultName = userRepository.save(userWithDefaultName);
            tokenDefaultName = jwtTokenService.generateToken(userWithDefaultName);

            // User with custom profile name
            userWithCustomName = new UserEntity();
            userWithCustomName.setEmail("comment_custom" + System.currentTimeMillis() + "@harvard.edu");
            userWithCustomName.setSchoolDomain("harvard.edu");
            userWithCustomName.setProfileName("Jane Smith");
            userWithCustomName.setVerified(true);
            userWithCustomName.setPasswordSet(true);
            userWithCustomName = userRepository.save(userWithCustomName);
            tokenCustomName = jwtTokenService.generateToken(userWithCustomName);

            // Create test post (post author doesn't matter for comment tests)
            testPost = new Post(userWithDefaultName.getId(), "Test post for comments", "campus", "harvard.edu");
            testPost = postRepository.save(testPost);
        }

        @Test
        @DisplayName("Should capture default profile name 'Anonymous' in comment")
        void shouldCaptureDefaultProfileNameInComment() {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("text", "Comment with default profile name");

            // Act
            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + testPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + tokenDefaultName),
                CommentDTO.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatus());
            CommentDTO commentDTO = response.body();
            assertNotNull(commentDTO.getAuthor());
            assertEquals("Anonymous", commentDTO.getAuthor().getProfileName());

            // Verify in database
            Optional<com.anonymous.wall.entity.Comment> savedComment =
                commentRepository.findById(commentDTO.getId());
            assertTrue(savedComment.isPresent());
            assertEquals("Anonymous", savedComment.get().getProfileName());
        }

        @Test
        @DisplayName("Should capture custom profile name in comment")
        void shouldCaptureCustomProfileNameInComment() {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("text", "Comment with custom profile name");

            // Act
            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + testPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + tokenCustomName),
                CommentDTO.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatus());
            CommentDTO commentDTO = response.body();
            assertNotNull(commentDTO.getAuthor());
            assertEquals("Jane Smith", commentDTO.getAuthor().getProfileName());

            // Verify in database
            Optional<com.anonymous.wall.entity.Comment> savedComment =
                commentRepository.findById(commentDTO.getId());
            assertTrue(savedComment.isPresent());
            assertEquals("Jane Smith", savedComment.get().getProfileName());
        }

        @Test
        @DisplayName("Should preserve original profile name after user changes name")
        void shouldPreserveOriginalProfileNameAfterUserChanges() {
            // Arrange - Create comment with initial profile name
            userWithDefaultName.setProfileName("Original Name");
            userRepository.update(userWithDefaultName);

            Map<String, Object> request = new HashMap<>();
            request.put("text", "Comment with original name");

            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + testPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + tokenDefaultName),
                CommentDTO.class
            );

            CommentDTO commentDTO = response.body();
            assertEquals("Original Name", commentDTO.getAuthor().getProfileName());

            // Now change user's profile name
            userWithDefaultName.setProfileName("Updated Name");
            userRepository.update(userWithDefaultName);

            // Verify comment still has original profile name
            Optional<com.anonymous.wall.entity.Comment> comment =
                commentRepository.findById(commentDTO.getId());
            assertTrue(comment.isPresent());
            assertEquals("Original Name", comment.get().getProfileName());

            // But new comments should use new name
            Map<String, Object> request2 = new HashMap<>();
            request2.put("text", "Another comment");

            HttpResponse<CommentDTO> response2 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + testPost.getId() + "/comments", request2)
                    .header("Authorization", "Bearer " + tokenDefaultName),
                CommentDTO.class
            );

            assertEquals("Updated Name", response2.body().getAuthor().getProfileName());
        }

        @Test
        @DisplayName("Comment author profile name should not equal user ID")
        void commentAuthorProfileNameShouldNotEqualUserId() {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("text", "Comment text");

            // Act
            HttpResponse<CommentDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + testPost.getId() + "/comments", request)
                    .header("Authorization", "Bearer " + tokenCustomName),
                CommentDTO.class
            );

            // Assert
            CommentDTO commentDTO = response.body();
            assertNotNull(commentDTO.getAuthor());
            assertEquals("Jane Smith", commentDTO.getAuthor().getProfileName());
            assertNotNull(commentDTO.getAuthor().getId());
            assertNotEquals("Jane Smith", commentDTO.getAuthor().getId());
        }

        @Test
        @DisplayName("Multiple comments from same user should show current profile name")
        void multipleCommentsFromSameUserWithDifferentNames() {
            // First comment with name "First Name"
            userWithDefaultName.setProfileName("First Name");
            userRepository.update(userWithDefaultName);

            Map<String, Object> request1 = new HashMap<>();
            request1.put("text", "First comment");

            HttpResponse<CommentDTO> response1 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + testPost.getId() + "/comments", request1)
                    .header("Authorization", "Bearer " + tokenDefaultName),
                CommentDTO.class
            );

            assertEquals("First Name", response1.body().getAuthor().getProfileName());

            // Update name
            userWithDefaultName.setProfileName("Second Name");
            userRepository.update(userWithDefaultName);

            Map<String, Object> request2 = new HashMap<>();
            request2.put("text", "Second comment");

            HttpResponse<CommentDTO> response2 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + testPost.getId() + "/comments", request2)
                    .header("Authorization", "Bearer " + tokenDefaultName),
                CommentDTO.class
            );

            assertEquals("Second Name", response2.body().getAuthor().getProfileName());

            // Verify both comments have their original names in database
            Optional<com.anonymous.wall.entity.Comment> comment1 =
                commentRepository.findById(response1.body().getId());
            Optional<com.anonymous.wall.entity.Comment> comment2 =
                commentRepository.findById(response2.body().getId());

            assertTrue(comment1.isPresent());
            assertTrue(comment2.isPresent());
            assertEquals("First Name", comment1.get().getProfileName());
            assertEquals("Second Name", comment2.get().getProfileName());
        }
    }
}
