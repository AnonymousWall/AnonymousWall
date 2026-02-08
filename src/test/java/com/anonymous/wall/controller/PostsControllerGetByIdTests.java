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

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Posts Controller - Get Post By ID Tests")
class PostsControllerGetByIdTests {

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

    private UserEntity testUserHarvard;
    private UserEntity testUserMIT;
    private UserEntity testUserNoSchool;
    private String jwtTokenHarvard;
    private String jwtTokenMIT;
    private String jwtTokenNoSchool;

    @BeforeEach
    void setUp() {
        // Clean up any leftover data
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();

        // Harvard student
        testUserHarvard = new UserEntity();
        testUserHarvard.setEmail("student" + System.currentTimeMillis() + "@harvard.edu");
        testUserHarvard.setSchoolDomain("harvard.edu");
        testUserHarvard.setVerified(true);
        testUserHarvard.setPasswordSet(true);
        testUserHarvard = userRepository.save(testUserHarvard);
        jwtTokenHarvard = jwtTokenService.generateToken(testUserHarvard);

        // MIT student
        testUserMIT = new UserEntity();
        testUserMIT.setEmail("student" + System.currentTimeMillis() + "@mit.edu");
        testUserMIT.setSchoolDomain("mit.edu");
        testUserMIT.setVerified(true);
        testUserMIT.setPasswordSet(true);
        testUserMIT = userRepository.save(testUserMIT);
        jwtTokenMIT = jwtTokenService.generateToken(testUserMIT);

        // Non-student
        testUserNoSchool = new UserEntity();
        testUserNoSchool.setEmail("user" + System.currentTimeMillis() + "@gmail.com");
        testUserNoSchool.setSchoolDomain(null);
        testUserNoSchool.setVerified(true);
        testUserNoSchool.setPasswordSet(true);
        testUserNoSchool = userRepository.save(testUserNoSchool);
        jwtTokenNoSchool = jwtTokenService.generateToken(testUserNoSchool);

        postRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // Must delete in order: likes, comments, then posts
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
    }

    @Nested
    @DisplayName("Get Post By ID - Success Tests")
    class GetPostByIdSuccessTests {

        @Test
        @DisplayName("Should retrieve national post by ID")
        void shouldRetrieveNationalPostById() {
            // Create a national post
            Post nationalPost = new Post(testUserHarvard.getId(), "National Post Title", "This is a national post", "national", null);
            nationalPost = postRepository.save(nationalPost);

            // Request the post by ID
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + nationalPost.getId())
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> body = response.body();
            assertNotNull(body);
            assertEquals(nationalPost.getId().toString(), body.get("id"));
            assertEquals("National Post Title", body.get("title"));
            assertEquals("This is a national post", body.get("content"));
            assertEquals("NATIONAL", body.get("wall"));
        }

        @Test
        @DisplayName("Should retrieve campus post by ID for same school user")
        void shouldRetrieveCampusPostByIdForSameSchool() {
            // Create a Harvard campus post
            Post campusPost = new Post(testUserHarvard.getId(), "Harvard Post Title", "This is a Harvard campus post", "campus", "harvard.edu");
            campusPost = postRepository.save(campusPost);

            // Request the post by ID as Harvard user
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + campusPost.getId())
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> body = response.body();
            assertNotNull(body);
            assertEquals(campusPost.getId().toString(), body.get("id"));
            assertEquals("Harvard Post Title", body.get("title"));
            assertEquals("This is a Harvard campus post", body.get("content"));
            assertEquals("CAMPUS", body.get("wall"));
        }

        @Test
        @DisplayName("Should include like and comment counts")
        void shouldIncludeLikeAndCommentCounts() {
            // Create a post with counts
            Post post = new Post(testUserHarvard.getId(), "Post Title", "Post content", "national", null);
            post.setLikeCount(5);
            post.setCommentCount(3);
            post = postRepository.save(post);

            // Request the post by ID
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + post.getId())
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> body = response.body();
            assertEquals(5, body.get("likes"));
            assertEquals(3, body.get("comments"));
        }

        @Test
        @DisplayName("Should include author information")
        void shouldIncludeAuthorInformation() {
            // Create a post
            Post post = new Post(testUserHarvard.getId(), "Post Title", "Post content", "national", null);
            post.setProfileName("TestUser");
            post = postRepository.save(post);

            // Request the post by ID
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + post.getId())
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> body = response.body();
            Map<String, Object> author = (Map<String, Object>) body.get("author");
            assertNotNull(author);
            assertEquals("TestUser", author.get("profileName"));
            assertTrue((Boolean) author.get("isAnonymous"));
        }
    }

    @Nested
    @DisplayName("Get Post By ID - Permission Tests")
    class GetPostByIdPermissionTests {

        @Test
        @DisplayName("Should deny access to campus post from different school")
        void shouldDenyAccessToDifferentSchoolCampusPost() {
            // Create a Harvard campus post
            Post harvardPost = new Post(testUserHarvard.getId(), "Harvard Post", "Harvard campus content", "campus", "harvard.edu");
            harvardPost = postRepository.save(harvardPost);

            // Try to access as MIT user
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + harvardPost.getId())
                        .header("Authorization", "Bearer " + jwtTokenMIT),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }

        @Test
        @DisplayName("Should allow access to national post from any school")
        void shouldAllowAccessToNationalPostFromAnySchool() {
            // Create a national post by Harvard user
            Post nationalPost = new Post(testUserHarvard.getId(), "National Post", "National content", "national", null);
            nationalPost = postRepository.save(nationalPost);

            // Access by MIT user
            HttpResponse<Map> responseMIT = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + nationalPost.getId())
                    .header("Authorization", "Bearer " + jwtTokenMIT),
                Map.class
            );

            assertEquals(HttpStatus.OK, responseMIT.getStatus());

            // Access by user without school
            HttpResponse<Map> responseNoSchool = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + nationalPost.getId())
                    .header("Authorization", "Bearer " + jwtTokenNoSchool),
                Map.class
            );

            assertEquals(HttpStatus.OK, responseNoSchool.getStatus());
        }

        @Test
        @DisplayName("Should deny access to campus post for user without school")
        void shouldDenyAccessToCampusPostForUserWithoutSchool() {
            // Create a Harvard campus post
            Post campusPost = new Post(testUserHarvard.getId(), "Campus Post", "Campus content", "campus", "harvard.edu");
            campusPost = postRepository.save(campusPost);

            // Try to access as user without school
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + campusPost.getId())
                        .header("Authorization", "Bearer " + jwtTokenNoSchool),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Get Post By ID - Error Tests")
    class GetPostByIdErrorTests {

        @Test
        @DisplayName("Should return 404 for non-existent post")
        void shouldReturn404ForNonExistentPost() {
            // Generate a random UUID that doesn't exist
            UUID nonExistentId = UUID.randomUUID();

            // Try to get the post
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + nonExistentId)
                        .header("Authorization", "Bearer " + jwtTokenHarvard),
                    Map.class
                )
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            // Create a post
            Post post = new Post(testUserHarvard.getId(), "Post Title", "Post content", "national", null);
            post = postRepository.save(post);

            // Try to access without authentication
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + post.getId()),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }
}
