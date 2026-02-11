package com.anonymous.wall.admin.controller;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.CommentRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Admin Post Controller Tests")
class AdminPostControllerTest {

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
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/admin/posts";

    private UserEntity adminUser;
    private UserEntity regularUser;
    private Post testPost;
    
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // Clean up
        commentRepository.deleteAll();
        postRepository.deleteAll();

        // Create admin user
        adminUser = new UserEntity();
        adminUser.setEmail("admin" + System.currentTimeMillis() + "@test.edu");
        adminUser.setSchoolDomain("test.edu");
        adminUser.setVerified(true);
        adminUser.setPasswordSet(true);
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);
        adminToken = jwtTokenService.generateToken(adminUser);

        // Create regular user
        regularUser = new UserEntity();
        regularUser.setEmail("user" + System.currentTimeMillis() + "@test.edu");
        regularUser.setSchoolDomain("test.edu");
        regularUser.setVerified(true);
        regularUser.setPasswordSet(true);
        regularUser.setRole("USER");
        regularUser = userRepository.save(regularUser);
        userToken = jwtTokenService.generateToken(regularUser);

        // Create test post
        testPost = new Post();
        testPost.setUserId(regularUser.getId());
        testPost.setTitle("Test Post");
        testPost.setContent("Test content");
        testPost.setWall("campus");
        testPost.setSchoolDomain("test.edu");
        testPost.setHidden(false);
        testPost = postRepository.save(testPost);
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
    }

    @Nested
    @DisplayName("List Posts Endpoint Tests")
    class ListPostsTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can list all posts")
        void adminCanListPosts() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH)
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            assertTrue(response.body().containsKey("pagination"));
            
            List<Map> posts = (List<Map>) response.body().get("data");
            assertTrue(posts.size() >= 1); // At least our test post
        }

        @Test
        @Order(2)
        @DisplayName("Negative: Regular user cannot list admin posts")
        void regularUserCannotListAdminPosts() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH)
                        .bearerAuth(userToken),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }

        @Test
        @Order(3)
        @DisplayName("Negative: Unauthenticated user cannot list admin posts")
        void unauthenticatedUserCannotListAdminPosts() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @Order(4)
        @DisplayName("Positive: Pagination works correctly")
        void paginationWorksCorrectly() {
            // Create more posts
            for (int i = 0; i < 5; i++) {
                Post post = new Post();
                post.setUserId(regularUser.getId());
                post.setTitle("Test Post " + i);
                post.setContent("Test content " + i);
                post.setWall("campus");
                post.setSchoolDomain("test.edu");
                postRepository.save(post);
            }

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?page=1&limit=3")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            Map pagination = (Map) response.body().get("pagination");
            assertEquals(1, pagination.get("page"));
            assertEquals(3, pagination.get("limit"));
        }
    }

    @Nested
    @DisplayName("Delete Post Endpoint Tests")
    class DeletePostTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can soft-delete a post")
        void adminCanSoftDeletePost() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.DELETE(BASE_PATH + "/" + testPost.getId())
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue(response.body().get("message").toString().contains("deleted"));

            // Verify in database - post should be hidden
            Post deletedPost = postRepository.findById(testPost.getId()).orElseThrow();
            assertTrue(deletedPost.isHidden());
        }

        @Test
        @Order(2)
        @DisplayName("Negative: Regular user cannot delete a post via admin endpoint")
        void regularUserCannotDeletePost() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.DELETE(BASE_PATH + "/" + testPost.getId())
                        .bearerAuth(userToken),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

            // Verify post is still not hidden
            Post post = postRepository.findById(testPost.getId()).orElseThrow();
            assertFalse(post.isHidden());
        }

        @Test
        @Order(3)
        @DisplayName("Negative: Unauthenticated user cannot delete a post")
        void unauthenticatedUserCannotDeletePost() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.DELETE(BASE_PATH + "/" + testPost.getId()),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @Order(4)
        @DisplayName("Negative: Admin gets error for non-existent post")
        void adminGetsErrorForNonExistentPost() {
            // Act & Assert
            UUID randomId = UUID.randomUUID();
            assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.DELETE(BASE_PATH + "/" + randomId)
                        .bearerAuth(adminToken),
                    Map.class
                )
            );
        }
    }
}
