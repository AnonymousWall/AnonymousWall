package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.CommentsService;
import com.anonymous.wall.service.JwtTokenService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("UserController Tests")
class UserControllerTest {

    private static final String BASE_PATH = "/api/v1/users/me/comments";

    @Inject
    @Client("/")
    private HttpClient client;

    @Inject
    private PostRepository postRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    @Inject
    private CommentsService commentsService;

    private UserEntity testUser1;
    private UserEntity testUser2;
    private String jwtToken1;
    private String jwtToken2;
    private Post testPost1;
    private Post testPost2;

    @BeforeEach
    void setUp() {
        // Clean up
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user 1
        testUser1 = new UserEntity();
        testUser1.setId(UUID.randomUUID());
        testUser1.setEmail("user1@harvard.edu");
        testUser1.setSchoolDomain("harvard.edu");
        testUser1.setProfileName("User1");
        testUser1.setVerified(true);
        testUser1.setPasswordSet(true);
        testUser1.setPasswordHash("dummy");
        testUser1 = userRepository.save(testUser1);

        jwtToken1 = jwtTokenService.generateToken(testUser1);

        // Create test user 2
        testUser2 = new UserEntity();
        testUser2.setId(UUID.randomUUID());
        testUser2.setEmail("user2@harvard.edu");
        testUser2.setSchoolDomain("harvard.edu");
        testUser2.setProfileName("User2");
        testUser2.setVerified(true);
        testUser2.setPasswordSet(true);
        testUser2.setPasswordHash("dummy");
        testUser2 = userRepository.save(testUser2);

        jwtToken2 = jwtTokenService.generateToken(testUser2);

        // Create test posts
        testPost1 = new Post(testUser1.getId(), "Title 1", "Test post 1", "campus", "harvard.edu");
        testPost1 = postRepository.save(testPost1);

        testPost2 = new Post(testUser2.getId(), "Title 2", "Test post 2", "campus", "harvard.edu");
        testPost2 = postRepository.save(testPost2);
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("User Own Comments - Basic Functionality")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Should return user's own comments")
        void shouldReturnUserOwnComments() {
            // User1 creates comments on both posts
            commentsService.addComment(testPost1.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("User1 comment on post1"), testUser1.getId());
            commentsService.addComment(testPost2.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("User1 comment on post2"), testUser1.getId());

            // User2 creates a comment
            commentsService.addComment(testPost1.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("User2 comment on post1"), testUser2.getId());

            String endpoint = BASE_PATH;
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatus());
            
            Map<String, Object> body = response.body();
            assertNotNull(body);
            
            List<Map> data = (List<Map>) body.get("data");
            assertNotNull(data);
            assertEquals(2, data.size(), "User1 should have 2 comments");

            // Verify comments belong to user1
            for (Map comment : data) {
                Map author = (Map) comment.get("author");
                assertEquals(testUser1.getId().toString(), author.get("id"));
            }
        }

        @Test
        @DisplayName("Should return empty list when user has no comments")
        void shouldReturnEmptyListWhenNoComments() {
            // User1 has no comments
            String endpoint = BASE_PATH;
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatus());
            
            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertNotNull(data);
            assertEquals(0, data.size());

            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(0, pagination.get("total"));
        }

        @Test
        @DisplayName("Should only return user's own comments, not others")
        void shouldOnlyReturnOwnComments() {
            // User1 creates 3 comments
            for (int i = 0; i < 3; i++) {
                commentsService.addComment(testPost1.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("User1 comment " + i), testUser1.getId());
            }

            // User2 creates 5 comments
            for (int i = 0; i < 5; i++) {
                commentsService.addComment(testPost1.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("User2 comment " + i), testUser2.getId());
            }

            // User1 should only see their 3 comments
            String endpoint = BASE_PATH;
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(3, data.size());

            // User2 should only see their 5 comments
            request = HttpRequest.GET(endpoint).bearerAuth(jwtToken2);
            response = client.toBlocking().exchange(request, Map.class);

            body = response.body();
            data = (List<Map>) body.get("data");
            assertEquals(5, data.size());
        }
    }

    @Nested
    @DisplayName("User Own Comments - Pagination")
    class PaginationTests {

        @Test
        @DisplayName("Should paginate comments correctly")
        void shouldPaginateCommentsCorrectly() {
            // Create 25 comments for user1
            for (int i = 0; i < 25; i++) {
                commentsService.addComment(testPost1.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment " + i), testUser1.getId());
            }

            // Get first page (default limit 20)
            String endpoint = BASE_PATH + "?page=1&limit=20";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(20, data.size());

            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(1, pagination.get("page"));
            assertEquals(20, pagination.get("limit"));
            assertEquals(25, pagination.get("total"));
            assertEquals(2, pagination.get("totalPages"));

            // Get second page
            endpoint = BASE_PATH + "?page=2&limit=20";
            request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            response = client.toBlocking().exchange(request, Map.class);

            body = response.body();
            data = (List<Map>) body.get("data");
            assertEquals(5, data.size());

            pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(2, pagination.get("page"));
        }

        @Test
        @DisplayName("Should handle custom page size")
        void shouldHandleCustomPageSize() {
            // Create 15 comments
            for (int i = 0; i < 15; i++) {
                commentsService.addComment(testPost1.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment " + i), testUser1.getId());
            }

            String endpoint = BASE_PATH + "?page=1&limit=5";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(5, data.size());

            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(5, pagination.get("limit"));
            assertEquals(3, pagination.get("totalPages"));
        }
    }

    @Nested
    @DisplayName("User Own Comments - Sorting")
    class SortingTests {

        @Test
        @DisplayName("Should sort comments by newest first (default)")
        void shouldSortByNewestFirst() throws InterruptedException {
            // Create comments with delays to ensure different timestamps
            for (int i = 1; i <= 3; i++) {
                commentsService.addComment(testPost1.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment " + i), testUser1.getId());
                Thread.sleep(1000); // 1 second delay to ensure different timestamps
            }

            String endpoint = BASE_PATH + "?sort=NEWEST";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(3, data.size());

            // Verify newest first (Comment 3, Comment 2, Comment 1)
            assertEquals("Comment 3", data.get(0).get("text"));
            assertEquals("Comment 2", data.get(1).get("text"));
            assertEquals("Comment 1", data.get(2).get("text"));
        }

        @Test
        @DisplayName("Should sort comments by oldest first")
        void shouldSortByOldestFirst() throws InterruptedException {
            // Create comments with delays
            for (int i = 1; i <= 3; i++) {
                commentsService.addComment(testPost1.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment " + i), testUser1.getId());
                Thread.sleep(1000); // 1 second delay to ensure different timestamps
            }

            String endpoint = BASE_PATH + "?sort=OLDEST";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(3, data.size());

            // Verify oldest first (Comment 1, Comment 2, Comment 3)
            assertEquals("Comment 1", data.get(0).get("text"));
            assertEquals("Comment 2", data.get(1).get("text"));
            assertEquals("Comment 3", data.get(2).get("text"));
        }
    }

    @Nested
    @DisplayName("User Own Comments - Hidden Comments")
    class HiddenCommentsTests {

        @Test
        @DisplayName("Should exclude hidden comments")
        void shouldExcludeHiddenCommentsByDefault() {
            // Create 3 comments
            Comment comment1 = commentsService.addComment(testPost1.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("Comment 1"), testUser1.getId());
            Comment comment2 = commentsService.addComment(testPost1.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("Comment 2"), testUser1.getId());
            commentsService.addComment(testPost1.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("Comment 3"), testUser1.getId());

            // Hide comment2
            commentsService.hideComment(testPost1.getId(), comment2.getId(), testUser1.getId());

            String endpoint = BASE_PATH;
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(2, data.size(), "Should only return non-hidden comments");

            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(2, pagination.get("total"));
        }
    }

    @Nested
    @DisplayName("User Own Comments - Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle invalid page parameter")
        void shouldHandleInvalidPageParameter() {
            commentsService.addComment(testPost1.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("Comment 1"), testUser1.getId());

            // Page 0 should default to 1
            String endpoint = BASE_PATH + "?page=0";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatus());
            
            Map<String, Object> body = response.body();
            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(1, pagination.get("page"));
        }

        @Test
        @DisplayName("Should handle limit out of bounds")
        void shouldHandleLimitOutOfBounds() {
            commentsService.addComment(testPost1.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("Comment 1"), testUser1.getId());

            // Limit > 100 should default to 20
            String endpoint = BASE_PATH + "?limit=200";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatus());
            
            Map<String, Object> body = response.body();
            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(20, pagination.get("limit"));
        }

        @Test
        @DisplayName("Should handle page beyond total pages")
        void shouldHandlePageBeyondTotal() {
            commentsService.addComment(testPost1.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("Comment 1"), testUser1.getId());

            // Request page 10 when there's only 1 page
            String endpoint = BASE_PATH + "?page=10";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatus());
            
            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(0, data.size());
        }
    }

    @Nested
    @DisplayName("User Own Comments - Performance")
    class PerformanceTests {

        @Test
        @DisplayName("Should efficiently retrieve large number of comments")
        void shouldEfficientlyRetrieveLargeNumberOfComments() {
            // Create 100 comments across multiple posts
            for (int i = 0; i < 50; i++) {
                commentsService.addComment(testPost1.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment on post1 - " + i), testUser1.getId());
            }
            for (int i = 0; i < 50; i++) {
                commentsService.addComment(testPost2.getId(),
                    new com.anonymous.wall.model.CreateCommentRequest("Comment on post2 - " + i), testUser1.getId());
            }

            // This should use a single optimized query with the composite index
            long startTime = System.currentTimeMillis();
            
            String endpoint = BASE_PATH + "?page=1&limit=100";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            assertEquals(HttpStatus.OK, response.getStatus());
            
            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(100, data.size());

            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(100, pagination.get("total"));

            // Performance assertion - should complete in reasonable time
            assertTrue(duration < 5000, "Query should complete in less than 5 seconds, took: " + duration + "ms");
        }
    }

    @Nested
    @DisplayName("Update Profile Name Endpoint Tests")
    class UpdateProfileNameTests {

        private static final String PROFILE_NAME_PATH = "/api/v1/users/me/profile/name";
        private static final int ASYNC_POLL_INTERVAL_MS = 100;
        private static final int ASYNC_POLL_MAX_ATTEMPTS = 50; // 50 * 100ms = 5 seconds max

        private UserEntity testUser;
        private String jwtToken;
        private UUID testUserId;

        @BeforeEach
        void setUp() {
            // Clean up
            commentRepository.deleteAll();
            postRepository.deleteAll();
            userRepository.deleteAll();

            testUser = new UserEntity();
            testUser.setEmail("profiletest" + System.currentTimeMillis() + "@harvard.edu");
            testUser.setProfileName("Anonymous");
            testUser.setVerified(true);
            testUser.setPasswordSet(true);
            testUser = userRepository.save(testUser);
            testUserId = testUser.getId();
            jwtToken = jwtTokenService.generateToken(testUser);
        }

        @Test
        @DisplayName("Positive: Should update profile name successfully")
        void shouldUpdateProfileName() {
            // Arrange
            com.anonymous.wall.model.UpdateProfileNameRequest request = new com.anonymous.wall.model.UpdateProfileNameRequest("John Doe");

            // Act
            HttpResponse<com.anonymous.wall.model.UserDTO> response = client.toBlocking().exchange(
                HttpRequest.PATCH(PROFILE_NAME_PATH, request)
                    .header("Authorization", "Bearer " + jwtToken),
                com.anonymous.wall.model.UserDTO.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            com.anonymous.wall.model.UserDTO body = response.body();
            assertNotNull(body);
            assertEquals("John Doe", body.getProfileName());
            assertEquals(testUser.getEmail(), body.getEmail());

            // Verify database was updated
            java.util.Optional<UserEntity> updatedUser = userRepository.findById(testUserId);
            assertTrue(updatedUser.isPresent());
            assertEquals("John Doe", updatedUser.get().getProfileName());
        }

        @Test
        @DisplayName("Positive: Should handle profile name with special characters")
        void shouldUpdateProfileNameWithSpecialCharacters() {
            // Arrange
            com.anonymous.wall.model.UpdateProfileNameRequest request = new com.anonymous.wall.model.UpdateProfileNameRequest("José García-López");

            // Act
            HttpResponse<com.anonymous.wall.model.UserDTO> response = client.toBlocking().exchange(
                HttpRequest.PATCH(PROFILE_NAME_PATH, request)
                    .header("Authorization", "Bearer " + jwtToken),
                com.anonymous.wall.model.UserDTO.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            com.anonymous.wall.model.UserDTO body = response.body();
            assertEquals("José García-López", body.getProfileName());
        }

        @Test
        @DisplayName("Positive: Should reset profile name to Anonymous with empty string")
        void shouldResetProfileNameToAnonymous() {
            // Arrange - First set a custom profile name
            com.anonymous.wall.model.UpdateProfileNameRequest setNameRequest = new com.anonymous.wall.model.UpdateProfileNameRequest("Custom Name");
            client.toBlocking().exchange(
                HttpRequest.PATCH(PROFILE_NAME_PATH, setNameRequest)
                    .header("Authorization", "Bearer " + jwtToken),
                com.anonymous.wall.model.UserDTO.class
            );

            // Now reset it with empty string
            com.anonymous.wall.model.UpdateProfileNameRequest resetRequest = new com.anonymous.wall.model.UpdateProfileNameRequest("");

            // Act
            HttpResponse<com.anonymous.wall.model.UserDTO> response = client.toBlocking().exchange(
                HttpRequest.PATCH(PROFILE_NAME_PATH, resetRequest)
                    .header("Authorization", "Bearer " + jwtToken),
                com.anonymous.wall.model.UserDTO.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            com.anonymous.wall.model.UserDTO body = response.body();
            assertEquals("Anonymous", body.getProfileName());

            // Verify database was updated
            java.util.Optional<UserEntity> updatedUser = userRepository.findById(testUserId);
            assertTrue(updatedUser.isPresent());
            assertEquals("Anonymous", updatedUser.get().getProfileName());
        }

        @Test
        @DisplayName("Positive: Should handle long profile name (max 255 chars)")
        void shouldHandleMaxLengthProfileName() {
            // Arrange
            String longName = "A".repeat(255);
            com.anonymous.wall.model.UpdateProfileNameRequest request = new com.anonymous.wall.model.UpdateProfileNameRequest(longName);

            // Act
            HttpResponse<com.anonymous.wall.model.UserDTO> response = client.toBlocking().exchange(
                HttpRequest.PATCH(PROFILE_NAME_PATH, request)
                    .header("Authorization", "Bearer " + jwtToken),
                com.anonymous.wall.model.UserDTO.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            com.anonymous.wall.model.UserDTO body = response.body();
            assertEquals(longName, body.getProfileName());
        }

        @Test
        @DisplayName("Positive: Should update profile name multiple times")
        void shouldUpdateProfileNameMultipleTimes() {
            // First update
            com.anonymous.wall.model.UpdateProfileNameRequest request1 = new com.anonymous.wall.model.UpdateProfileNameRequest("First Name");
            HttpResponse<com.anonymous.wall.model.UserDTO> response1 = client.toBlocking().exchange(
                HttpRequest.PATCH(PROFILE_NAME_PATH, request1)
                    .header("Authorization", "Bearer " + jwtToken),
                com.anonymous.wall.model.UserDTO.class
            );
            assertEquals("First Name", response1.body().getProfileName());

            // Second update
            com.anonymous.wall.model.UpdateProfileNameRequest request2 = new com.anonymous.wall.model.UpdateProfileNameRequest("Second Name");
            HttpResponse<com.anonymous.wall.model.UserDTO> response2 = client.toBlocking().exchange(
                HttpRequest.PATCH(PROFILE_NAME_PATH, request2)
                    .header("Authorization", "Bearer " + jwtToken),
                com.anonymous.wall.model.UserDTO.class
            );
            assertEquals("Second Name", response2.body().getProfileName());

            // Verify final state in database
            java.util.Optional<UserEntity> updatedUser = userRepository.findById(testUserId);
            assertTrue(updatedUser.isPresent());
            assertEquals("Second Name", updatedUser.get().getProfileName());
        }

        @Test
        @DisplayName("Negative: Should reject request without authentication")
        void shouldRejectWithoutAuthentication() {
            // Arrange
            com.anonymous.wall.model.UpdateProfileNameRequest request = new com.anonymous.wall.model.UpdateProfileNameRequest("John Doe");

            // Act & Assert
            io.micronaut.http.client.exceptions.HttpClientResponseException exception = assertThrows(
                io.micronaut.http.client.exceptions.HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(PROFILE_NAME_PATH, request),
                    com.anonymous.wall.model.UserDTO.class
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should reject request with invalid authentication")
        void shouldRejectWithInvalidAuthentication() {
            // Arrange
            com.anonymous.wall.model.UpdateProfileNameRequest request = new com.anonymous.wall.model.UpdateProfileNameRequest("John Doe");

            // Act & Assert
            io.micronaut.http.client.exceptions.HttpClientResponseException exception = assertThrows(
                io.micronaut.http.client.exceptions.HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(PROFILE_NAME_PATH, request)
                        .header("Authorization", "Bearer invalid-token"),
                    com.anonymous.wall.model.UserDTO.class
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should reject profile name exceeding max length")
        void shouldRejectProfileNameExceedingMaxLength() {
            // Arrange - 256 characters (exceeds max of 255)
            String tooLongName = "A".repeat(256);
            com.anonymous.wall.model.UpdateProfileNameRequest request = new com.anonymous.wall.model.UpdateProfileNameRequest(tooLongName);

            // Act & Assert
            io.micronaut.http.client.exceptions.HttpClientResponseException exception = assertThrows(
                io.micronaut.http.client.exceptions.HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(PROFILE_NAME_PATH, request)
                        .header("Authorization", "Bearer " + jwtToken),
                    com.anonymous.wall.model.UserDTO.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Positive: Profile name should persist across requests")
        void shouldPersistProfileNameAcrossRequests() {
            // Arrange
            com.anonymous.wall.model.UpdateProfileNameRequest request = new com.anonymous.wall.model.UpdateProfileNameRequest("Persistent Name");

            // Act - First request to update
            HttpResponse<com.anonymous.wall.model.UserDTO> response1 = client.toBlocking().exchange(
                HttpRequest.PATCH(PROFILE_NAME_PATH, request)
                    .header("Authorization", "Bearer " + jwtToken),
                com.anonymous.wall.model.UserDTO.class
            );

            assertEquals("Persistent Name", response1.body().getProfileName());

            // Now verify by fetching the user again through a new token
            // (Simulating a new request session)
            java.util.Optional<UserEntity> refreshedUser = userRepository.findById(testUserId);
            assertTrue(refreshedUser.isPresent());
            assertEquals("Persistent Name", refreshedUser.get().getProfileName());
        }

        @Test
        @DisplayName("Integration: Should asynchronously propagate profile name to posts and comments")
        void shouldAsyncPropagateProfileNameToPostsAndComments() throws InterruptedException {
            // Arrange - Create posts and comments with current profile name
            Post testPost = new Post(testUserId, "Test Post", "Content", "campus", "harvard.edu");
            testPost.setProfileName(testUser.getProfileName());
            testPost = postRepository.save(testPost);

            Comment testComment = new Comment(testPost.getId(), testUserId, "Test Comment");
            testComment.setProfileName(testUser.getProfileName());
            testComment = commentRepository.save(testComment);

            String oldProfileName = testUser.getProfileName();
            assertEquals("Anonymous", oldProfileName);

            // Act - Update profile name
            com.anonymous.wall.model.UpdateProfileNameRequest request = new com.anonymous.wall.model.UpdateProfileNameRequest("New Profile Name");
            HttpResponse<com.anonymous.wall.model.UserDTO> response = client.toBlocking().exchange(
                HttpRequest.PATCH(PROFILE_NAME_PATH, request)
                    .header("Authorization", "Bearer " + jwtToken),
                com.anonymous.wall.model.UserDTO.class
            );

            // Assert - User updated immediately
            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals("New Profile Name", response.body().getProfileName());

            // Wait for async event processing with polling (max 5 seconds)
            boolean postUpdated = false;
            boolean commentUpdated = false;
            int attempts = 0;

            while ((!postUpdated || !commentUpdated) && attempts < ASYNC_POLL_MAX_ATTEMPTS) {
                Thread.sleep(ASYNC_POLL_INTERVAL_MS);
                attempts++;

                java.util.Optional<Post> updatedPost = postRepository.findById(testPost.getId());
                if (updatedPost.isPresent() && "New Profile Name".equals(updatedPost.get().getProfileName())) {
                    postUpdated = true;
                }

                java.util.Optional<Comment> updatedComment = commentRepository.findById(testComment.getId());
                if (updatedComment.isPresent() && "New Profile Name".equals(updatedComment.get().getProfileName())) {
                    commentUpdated = true;
                }
            }

            // Verify posts and comments were updated asynchronously
            assertTrue(postUpdated, 
                "Post profile name should be updated via async event within 5 seconds");
            assertTrue(commentUpdated, 
                "Comment profile name should be updated via async event within 5 seconds");
        }
    }
}
