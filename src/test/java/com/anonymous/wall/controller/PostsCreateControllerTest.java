package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.PostDTO;
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

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Posts Controller - Create Post Tests")
class PostsCreateControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    PostRepository postRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/posts";

    private UserEntity testUserCampus;
    private UserEntity testUserDifferentSchool;
    private String jwtTokenCampus;
    private String jwtTokenDifferentSchool;

    @BeforeEach
    void setUp() {
        // All users register with school email and have school domain
        testUserCampus = new UserEntity();
        testUserCampus.setEmail("student" + System.currentTimeMillis() + "@harvard.edu");
        testUserCampus.setSchoolDomain("harvard.edu");
        testUserCampus.setVerified(true);
        testUserCampus.setPasswordSet(true);
        testUserCampus = userRepository.save(testUserCampus);
        jwtTokenCampus = jwtTokenService.generateToken(testUserCampus);

        // User from different school - also has school domain
        testUserDifferentSchool = new UserEntity();
        testUserDifferentSchool.setEmail("student" + System.currentTimeMillis() + "@mit.edu");
        testUserDifferentSchool.setSchoolDomain("mit.edu");
        testUserDifferentSchool.setVerified(true);
        testUserDifferentSchool.setPasswordSet(true);
        testUserDifferentSchool = userRepository.save(testUserDifferentSchool);
        jwtTokenDifferentSchool = jwtTokenService.generateToken(testUserDifferentSchool);

        postRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create Post - Positive Cases")
    class CreatePostPositiveTests {

        @Test
        @DisplayName("Should create campus post with valid content")
        void shouldCreateCampusPost() {
            Map<String, Object> request = new HashMap<>();
            request.put("content", "This is a great campus post!");
            request.put("wall", "campus");

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            PostDTO body = response.body();
            assertNotNull(body);
            assertEquals("This is a great campus post!", body.getContent());
            assertEquals("campus", body.getWall().toString().toLowerCase());
            assertNotNull(body.getId());
            assertTrue(body.getAuthor().getIsAnonymous());
            assertEquals(0, body.getLikes());
            assertEquals(0, body.getComments());

            Optional<Post> savedPost = postRepository.findById(body.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("This is a great campus post!", savedPost.get().getContent());
            assertEquals("campus", savedPost.get().getWall());
            assertEquals("harvard.edu", savedPost.get().getSchoolDomain());
            assertEquals(testUserCampus.getId(), savedPost.get().getUserId());
        }

        @Test
        @DisplayName("Should create national post with valid content")
        void shouldCreateNationalPost() {
            Map<String, Object> request = new HashMap<>();
            request.put("content", "This is a national post visible to everyone!");
            request.put("wall", "national");

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            PostDTO body = response.body();
            assertNotNull(body);
            assertEquals("This is a national post visible to everyone!", body.getContent());
            assertEquals("national", body.getWall().toString().toLowerCase());

            Optional<Post> savedPost = postRepository.findById(body.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("national", savedPost.get().getWall());
        }

        @Test
        @DisplayName("Should default to campus wall when not specified")
        void shouldDefaultToCampusWall() {
            Map<String, Object> request = new HashMap<>();
            request.put("content", "Default campus post");

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("campus", response.body().getWall().toString().toLowerCase());

            Optional<Post> savedPost = postRepository.findById(response.body().getId());
            assertTrue(savedPost.isPresent());
            assertEquals("campus", savedPost.get().getWall());
        }

        @Test
        @DisplayName("Should create post with special characters")
        void shouldCreatePostWithSpecialCharacters() {
            String content = "Check this out! @everyone #campus 💯 This is awesome🎉";
            Map<String, Object> request = new HashMap<>();
            request.put("content", content);

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals(content, response.body().getContent());
        }

        @Test
        @DisplayName("Should create multiple posts from same user")
        void shouldCreateMultiplePostsFromSameUser() {
            Map<String, Object> request1 = new HashMap<>();
            request1.put("content", "First post");

            Map<String, Object> request2 = new HashMap<>();
            request2.put("content", "Second post");

            HttpResponse<PostDTO> response1 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request1)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                PostDTO.class
            );
            HttpResponse<PostDTO> response2 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request2)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response1.getStatus());
            assertEquals(HttpStatus.CREATED, response2.getStatus());
            assertNotEquals(response1.body().getId(), response2.body().getId());
            assertEquals("First post", response1.body().getContent());
            assertEquals("Second post", response2.body().getContent());
        }
    }

    @Nested
    @DisplayName("Create Post - Negative Cases")
    class CreatePostNegativeTests {

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuthentication() {
            Map<String, Object> request = new HashMap<>();
            request.put("content", "Unauthorized post");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request),
                    PostDTO.class
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail with empty content")
        void shouldFailWithEmptyContent() {
            Map<String, Object> request = new HashMap<>();
            request.put("content", "");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request)
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when content exceeds maximum length")
        void shouldFailWithContentTooLong() {
            String content = "X".repeat(5001);
            Map<String, Object> request = new HashMap<>();
            request.put("content", content);

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request)
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail with invalid wall type")
        void shouldFailWithInvalidWallType() {
            Map<String, Object> request = new HashMap<>();
            request.put("content", "Post to invalid wall");
            request.put("wall", "invalid");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request)
                        .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Create Post - Edge Cases")
    class CreatePostEdgeCaseTests {

        @Test
        @DisplayName("Should fail when user without school domain tries to post to campus")
        void shouldFailCampusPostWithoutSchoolDomain() {
            UserEntity userNoSchool = new UserEntity();
            userNoSchool.setEmail("noschool" + System.currentTimeMillis() + "@gmail.com");
            userNoSchool.setSchoolDomain(null);
            userNoSchool.setVerified(true);
            userNoSchool.setPasswordSet(true);
            userNoSchool = userRepository.save(userNoSchool);
            String tokenNoSchool = jwtTokenService.generateToken(userNoSchool);

            Map<String, Object> request = new HashMap<>();
            request.put("content", "Campus post without school domain");
            request.put("wall", "campus");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request)
                        .header("Authorization", "Bearer " + tokenNoSchool),
                    PostDTO.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should allow national post even without school domain")
        void shouldAllowNationalPostWithoutSchoolDomain() {
            UserEntity userNoSchool = new UserEntity();
            userNoSchool.setEmail("noschool" + System.currentTimeMillis() + "@gmail.com");
            userNoSchool.setSchoolDomain(null);
            userNoSchool.setVerified(true);
            userNoSchool.setPasswordSet(true);
            userNoSchool = userRepository.save(userNoSchool);
            String tokenNoSchool = jwtTokenService.generateToken(userNoSchool);

            Map<String, Object> request = new HashMap<>();
            request.put("content", "National post from user without school");
            request.put("wall", "national");

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + tokenNoSchool),
                PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("national", response.body().getWall().toString().toLowerCase());
        }

        @Test
        @DisplayName("Should create post with maximum length (5000 characters)")
        void shouldCreatePostWithMaximumLength() {
            String content = "X".repeat(5000);
            Map<String, Object> request = new HashMap<>();
            request.put("content", content);

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals(5000, response.body().getContent().length());
        }

        @Test
        @DisplayName("Should create post with minimum length (1 character)")
        void shouldCreatePostWithMinimumLength() {
            Map<String, Object> request = new HashMap<>();
            request.put("content", "A");

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("A", response.body().getContent());
        }
    }

    @Nested
    @DisplayName("Create Post - Security Tests")
    class CreatePostSecurityTests {

        @Test
        @DisplayName("Should anonymize user in response")
        void shouldAnonymizeUser() {
            Map<String, Object> request = new HashMap<>();
            request.put("content", "Test anonymity");

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                PostDTO.class
            );

            assertTrue(response.body().getAuthor().getIsAnonymous());
            assertNotNull(response.body().getAuthor().getId());
        }

        @Test
        @DisplayName("Post should be created with correct user ID in database")
        void shouldStoreCorrectUserIdInDatabase() {
            Map<String, Object> request = new HashMap<>();
            request.put("content", "Verify correct user");

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                PostDTO.class
            );

            Optional<Post> savedPost = postRepository.findById(response.body().getId());
            assertTrue(savedPost.isPresent());
            assertEquals(testUserCampus.getId(), savedPost.get().getUserId());
        }

        @Test
        @DisplayName("Different users should have separate posts")
        void shouldKeepUsersPostsSeparate() {
            Map<String, Object> request1 = new HashMap<>();
            request1.put("content", "User 1 post");
            Map<String, Object> request2 = new HashMap<>();
            request2.put("content", "User 2 post");

            HttpResponse<PostDTO> response1 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request1)
                    .header("Authorization", "Bearer " + jwtTokenCampus),
                PostDTO.class
            );
            HttpResponse<PostDTO> response2 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request2)
                    .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                PostDTO.class
            );

            Optional<Post> post1 = postRepository.findById(response1.body().getId());
            Optional<Post> post2 = postRepository.findById(response2.body().getId());
            assertTrue(post1.isPresent());
            assertTrue(post2.isPresent());
            assertEquals(testUserCampus.getId(), post1.get().getUserId());
            assertEquals(testUserDifferentSchool.getId(), post2.get().getUserId());
        }
    }

    @Nested
    @DisplayName("Create Post - Profile Name Tests")
    class CreatePostProfileNameTests {

        private UserEntity userWithDefaultName;
        private UserEntity userWithCustomName;
        private String tokenDefaultName;
        private String tokenCustomName;

        @BeforeEach
        void setUp() {
            // User with default profile name
            userWithDefaultName = new UserEntity();
            userWithDefaultName.setEmail("default" + System.currentTimeMillis() + "@harvard.edu");
            userWithDefaultName.setSchoolDomain("harvard.edu");
            userWithDefaultName.setProfileName("Anonymous");
            userWithDefaultName.setVerified(true);
            userWithDefaultName.setPasswordSet(true);
            userWithDefaultName = userRepository.save(userWithDefaultName);
            tokenDefaultName = jwtTokenService.generateToken(userWithDefaultName);

            // User with custom profile name
            userWithCustomName = new UserEntity();
            userWithCustomName.setEmail("custom" + System.currentTimeMillis() + "@harvard.edu");
            userWithCustomName.setSchoolDomain("harvard.edu");
            userWithCustomName.setProfileName("John Doe");
            userWithCustomName.setVerified(true);
            userWithCustomName.setPasswordSet(true);
            userWithCustomName = userRepository.save(userWithCustomName);
            tokenCustomName = jwtTokenService.generateToken(userWithCustomName);
        }

        @Test
        @DisplayName("Should capture default profile name 'Anonymous' in post")
        void shouldCaptureDefaultProfileNameInPost() {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("content", "Post with default profile name");
            request.put("wall", "campus");

            // Act
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + tokenDefaultName),
                PostDTO.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatus());
            PostDTO postDTO = response.body();
            assertNotNull(postDTO.getAuthor());
            assertEquals("Anonymous", postDTO.getAuthor().getProfileName());

            // Verify in database
            Optional<Post> savedPost = postRepository.findById(postDTO.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("Anonymous", savedPost.get().getProfileName());
        }

        @Test
        @DisplayName("Should capture custom profile name in post")
        void shouldCaptureCustomProfileNameInPost() {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("content", "Post with custom profile name");
            request.put("wall", "campus");

            // Act
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + tokenCustomName),
                PostDTO.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatus());
            PostDTO postDTO = response.body();
            assertNotNull(postDTO.getAuthor());
            assertEquals("John Doe", postDTO.getAuthor().getProfileName());

            // Verify in database
            Optional<Post> savedPost = postRepository.findById(postDTO.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("John Doe", savedPost.get().getProfileName());
        }

        @Test
        @DisplayName("Should preserve original profile name after user changes name")
        void shouldPreserveOriginalProfileNameAfterUserChanges() {
            // Arrange - Create post with initial profile name "Custom Name"
            userWithDefaultName.setProfileName("Custom Name");
            userRepository.update(userWithDefaultName);

            Map<String, Object> request = new HashMap<>();
            request.put("content", "Post with custom name");
            request.put("wall", "campus");

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + tokenDefaultName),
                PostDTO.class
            );

            PostDTO postDTO = response.body();
            assertEquals("Custom Name", postDTO.getAuthor().getProfileName());

            // Now change user's profile name
            userWithDefaultName.setProfileName("New Name");
            userRepository.update(userWithDefaultName);

            // Verify post still has original profile name
            Optional<Post> post = postRepository.findById(postDTO.getId());
            assertTrue(post.isPresent());
            assertEquals("Custom Name", post.get().getProfileName());

            // But new posts should use new name
            Map<String, Object> request2 = new HashMap<>();
            request2.put("content", "New post");
            request2.put("wall", "campus");

            HttpResponse<PostDTO> response2 = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request2)
                    .header("Authorization", "Bearer " + tokenDefaultName),
                PostDTO.class
            );

            assertEquals("New Name", response2.body().getAuthor().getProfileName());
        }

        @Test
        @DisplayName("Post author profile name should not equal user ID")
        void postAuthorProfileNameShouldNotEqualUserId() {
            // Arrange
            Map<String, Object> request = new HashMap<>();
            request.put("content", "Post content");
            request.put("wall", "campus");

            // Act
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + tokenCustomName),
                PostDTO.class
            );

            // Assert
            PostDTO postDTO = response.body();
            assertNotNull(postDTO.getAuthor());
            assertEquals("John Doe", postDTO.getAuthor().getProfileName());
            assertNotNull(postDTO.getAuthor().getId());
            assertNotEquals("John Doe", postDTO.getAuthor().getId());
        }
    }
}
