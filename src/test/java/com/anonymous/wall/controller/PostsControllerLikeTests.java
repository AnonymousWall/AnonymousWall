package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
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
@DisplayName("Posts Controller - Like/Unlike Tests")
class PostsControllerLikeTests {

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
        // Clean up any leftover data
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();

        // Harvard student
        testUserCampus = new UserEntity();
        testUserCampus.setEmail("student" + System.currentTimeMillis() + "@harvard.edu");
        testUserCampus.setSchoolDomain("harvard.edu");
        testUserCampus.setVerified(true);
        testUserCampus.setPasswordSet(true);
        testUserCampus = userRepository.save(testUserCampus);
        jwtTokenCampus = jwtTokenService.generateToken(testUserCampus);

        // MIT student
        testUserDifferentSchool = new UserEntity();
        testUserDifferentSchool.setEmail("student" + System.currentTimeMillis() + "@mit.edu");
        testUserDifferentSchool.setSchoolDomain("mit.edu");
        testUserDifferentSchool.setVerified(true);
        testUserDifferentSchool.setPasswordSet(true);
        testUserDifferentSchool = userRepository.save(testUserDifferentSchool);
        jwtTokenDifferentSchool = jwtTokenService.generateToken(testUserDifferentSchool);

        // Non-student
        testUserNoSchool = new UserEntity();
        testUserNoSchool.setEmail("user" + System.currentTimeMillis() + "@gmail.com");
        testUserNoSchool.setSchoolDomain(null);
        testUserNoSchool.setVerified(true);
        testUserNoSchool.setPasswordSet(true);
        testUserNoSchool = userRepository.save(testUserNoSchool);
        jwtTokenNoSchool = jwtTokenService.generateToken(testUserNoSchool);

        // Create test posts
        campusPost = new Post(testUserCampus.getId(), "Title", "Harvard campus post", "campus", "harvard.edu");
        campusPost = postRepository.save(campusPost);

        nationalPost = new Post(testUserCampus.getId(), "Title", "National post", "national", null);
        nationalPost = postRepository.save(nationalPost);
    }

    @AfterEach
    void tearDown() {
        // Must delete in order: likes, comments, then posts
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
    }

    @Nested
    @DisplayName("Toggle Like - Positive Cases")
    class ToggleLikePositiveTests {

        @Test
        @DisplayName("Should like a post")
        void shouldLikePost() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            assertNotNull(body);
            assertTrue((Boolean) body.get("liked"));
            assertNotNull(body.get("likeCount"));
            assertTrue(((Number) body.get("likeCount")).longValue() >= 1);
        }

        @Test
        @DisplayName("Should unlike a post (toggle)")
        void shouldUnlikePost() {
            // First like
            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            // Then unlike (toggle)
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            assertNotNull(body);
            assertFalse((Boolean) body.get("liked"));
        }

        @Test
        @DisplayName("Should handle multiple like/unlike cycles")
        void shouldHandleMultipleCycles() {
            // Like
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            assertTrue((Boolean) response1.body().get("liked"));

            // Unlike
            HttpResponse<Map> response2 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            assertFalse((Boolean) response2.body().get("liked"));

            // Like again
            HttpResponse<Map> response3 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            assertTrue((Boolean) response3.body().get("liked"));
        }

        @Test
        @DisplayName("Should like national post")
        void shouldLikeNationalPost() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + nationalPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue((Boolean) response.body().get("liked"));
        }
    }

    @Nested
    @DisplayName("Toggle Like - Negative Cases")
    class ToggleLikeNegativeTests {

        @Test
        @DisplayName("Should fail like on non-existent post")
        void shouldFailOnMissingPost() {
            UUID nonExistentPostId = UUID.randomUUID();
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + nonExistentPostId + "/likes", new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
                )
            );
            // Endpoint might return FORBIDDEN or NOT_FOUND
            assertTrue(
                exception.getStatus().equals(HttpStatus.NOT_FOUND) ||
                exception.getStatus().equals(HttpStatus.FORBIDDEN),
                "Expected NOT_FOUND or FORBIDDEN for missing post"
            );
        }

        @Test
        @DisplayName("Should reject like without authentication")
        void shouldRejectUnauthenticatedLike() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>()),
                    Map.class
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Toggle Like - Permission Tests")
    class ToggleLikePermissionTests {

        @Test
        @DisplayName("Should deny like from different school on campus post")
        void shouldDenyLikeFromDifferentSchool() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                    Map.class
                )
            );
            assertTrue(
                exception.getStatus().equals(HttpStatus.FORBIDDEN) ||
                exception.getStatus().equals(HttpStatus.BAD_REQUEST),
                "Expected FORBIDDEN or BAD_REQUEST for cross-school like"
            );
        }

        @Test
        @DisplayName("Should allow like on national post from different school")
        void shouldAllowLikeOnNationalPostDifferentSchool() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + nationalPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue((Boolean) response.body().get("liked"));
        }

        @Test
        @DisplayName("Should deny like from user without school on campus post")
        void shouldDenyLikeFromNoSchoolUserOnCampus() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                        .header("Authorization", "Bearer " + jwtTokenNoSchool),
                    Map.class
                )
            );
            assertTrue(
                exception.getStatus().equals(HttpStatus.FORBIDDEN) ||
                exception.getStatus().equals(HttpStatus.BAD_REQUEST),
                "Expected FORBIDDEN or BAD_REQUEST for no-school user"
            );
        }

        @Test
        @DisplayName("Should allow like on national post from user without school")
        void shouldAllowLikeOnNationalPostNoSchool() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + nationalPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenNoSchool),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue((Boolean) response.body().get("liked"));
        }
    }

    @Nested
    @DisplayName("Like Count - Data Tests")
    class LikeCountTests {

        @Test
        @DisplayName("Should track like count accurately")
        void shouldTrackLikeCountAccurately() {
            // Like from 3 different users (all from Harvard)
            UserEntity user2 = new UserEntity();
            user2.setEmail("student2" + System.currentTimeMillis() + "@harvard.edu");
            user2.setSchoolDomain("harvard.edu");
            user2.setVerified(true);
            user2.setPasswordSet(true);
            user2 = userRepository.save(user2);
            String token2 = jwtTokenService.generateToken(user2);

            UserEntity user3 = new UserEntity();
            user3.setEmail("student3" + System.currentTimeMillis() + "@harvard.edu");
            user3.setSchoolDomain("harvard.edu");
            user3.setVerified(true);
            user3.setPasswordSet(true);
            user3 = userRepository.save(user3);
            String token3 = jwtTokenService.generateToken(user3);

            // User 1 likes
            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            // User 2 likes
            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + token2),
                Map.class
            );

            // User 3 likes
            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + token3),
                Map.class
            );

            // Verify count is at least 3
            Optional<Post> updatedPost = postRepository.findById(campusPost.getId());
            assertTrue(updatedPost.isPresent());
            assertTrue(updatedPost.get().getLikeCount() >= 3);
        }

        @Test
        @DisplayName("Should show correct like status for current user")
        void shouldShowCorrectLikeStatus() {
            // User 1 likes the post
            HttpResponse<Map> likeResponse = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            // User 1 should see liked=true
            assertTrue((Boolean) likeResponse.body().get("liked"));

            // Other users shouldn't like, so different post fetch would show different status
            // (This would need a GET endpoint to test fully)
        }

        @Test
        @DisplayName("Like count should decrease when unlike")
        void shouldDecreaseLikeCountOnUnlike() {
            // Like
            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            Optional<Post> afterLike = postRepository.findById(campusPost.getId());
            assertTrue(afterLike.isPresent());
            long countAfterLike = afterLike.get().getLikeCount();
            assertTrue(countAfterLike >= 1);

            // Unlike
            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );

            Optional<Post> afterUnlike = postRepository.findById(campusPost.getId());
            assertTrue(afterUnlike.isPresent());
            long countAfterUnlike = afterUnlike.get().getLikeCount();
            assertTrue(countAfterUnlike < countAfterLike);
        }
    }

    @Nested
    @DisplayName("Like Consistency - Edge Cases")
    class LikeConsistencyTests {

        @Test
        @DisplayName("Should not create duplicate likes")
        void shouldNotCreateDuplicateLikes() {
            // Like the post
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            long count1 = ((Number) response1.body().get("likeCount")).longValue();

            // Try to like again (should toggle/return no-op or same count)
            HttpResponse<Map> response2 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            long count2 = ((Number) response2.body().get("likeCount")).longValue();

            // Either count stays same or it toggled (unliked)
            assertTrue(count2 <= count1, "Like count should not increase on second like");
        }

        @Test
        @DisplayName("Should maintain like state across requests")
        void shouldMaintainLikeState() {
            // Like post
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            assertTrue((Boolean) response1.body().get("liked"));

            // Verify state persists by liking again (which should unlike)
            HttpResponse<Map> response2 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            assertFalse((Boolean) response2.body().get("liked"), "Toggle should set liked to false");

            // Like again to verify
            HttpResponse<Map> response3 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + campusPost.getId() + "/likes", new HashMap<>())
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                Map.class
            );
            assertTrue((Boolean) response3.body().get("liked"), "Toggle should set liked to true again");
        }
    }
}
