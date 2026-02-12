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

    @Nested
    @DisplayName("Post Sorting and Filtering Tests")
    class PostSortingAndFilteringTests {

        @Test
        @DisplayName("Positive: Sort posts by creation time descending")
        void sortPostsByCreatedAtDesc() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=createdAt&sortOrder=desc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Sort posts by creation time ascending")
        void sortPostsByCreatedAtAsc() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=createdAt&sortOrder=asc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Sort posts by like count descending")
        void sortPostsByLikeCountDesc() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=likeCount&sortOrder=desc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Sort posts by like count ascending")
        void sortPostsByLikeCountAsc() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=likeCount&sortOrder=asc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Sort posts by comment count descending")
        void sortPostsByCommentCountDesc() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=commentCount&sortOrder=desc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Sort posts by author (userId)")
        void sortPostsByAuthor() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=userId")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Filter posts by author userId")
        void filterPostsByUserId() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?userId=" + regularUser.getId())
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Filter posts by hidden status")
        void filterPostsByHidden() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?hidden=false")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Combine filtering and sorting")
        void combineFilteringAndSorting() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?hidden=false&sortBy=likeCount&sortOrder=desc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }
    }

    @Nested
    @DisplayName("Post By Wall Endpoint Tests")
    class PostsByWallTests {

        @BeforeEach
        void setupWallTests() {
            // Create national posts
            Post nationalPost1 = new Post();
            nationalPost1.setUserId(regularUser.getId());
            nationalPost1.setTitle("National Post 1");
            nationalPost1.setContent("National content 1");
            nationalPost1.setWall("national");
            nationalPost1.setSchoolDomain(null);
            nationalPost1.setLikeCount(5);
            postRepository.save(nationalPost1);

            Post nationalPost2 = new Post();
            nationalPost2.setUserId(regularUser.getId());
            nationalPost2.setTitle("National Post 2");
            nationalPost2.setContent("National content 2");
            nationalPost2.setWall("national");
            nationalPost2.setSchoolDomain(null);
            nationalPost2.setLikeCount(10);
            postRepository.save(nationalPost2);

            // Create campus posts
            Post campusPost1 = new Post();
            campusPost1.setUserId(regularUser.getId());
            campusPost1.setTitle("Campus Post 1");
            campusPost1.setContent("Campus content 1");
            campusPost1.setWall("campus");
            campusPost1.setSchoolDomain("test.edu");
            campusPost1.setLikeCount(3);
            postRepository.save(campusPost1);
        }

        @Test
        @DisplayName("Positive: Admin can get all national posts with default sorting")
        void adminCanGetNationalPosts() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/by-wall?wall=national")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            assertTrue(response.body().containsKey("pagination"));
            
            List<Map> posts = (List<Map>) response.body().get("data");
            assertTrue(posts.size() >= 2); // At least 2 national posts
            
            // Verify all posts are national
            for (Map post : posts) {
                assertEquals("national", post.get("wall"));
            }
        }

        @Test
        @DisplayName("Positive: Admin can get all campus posts with default sorting")
        void adminCanGetCampusPosts() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/by-wall?wall=campus")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            
            List<Map> posts = (List<Map>) response.body().get("data");
            assertTrue(posts.size() >= 2); // At least 2 campus posts (including test post)
            
            // Verify all posts are campus
            for (Map post : posts) {
                assertEquals("campus", post.get("wall"));
            }
        }

        @Test
        @DisplayName("Positive: Admin can get all posts when wall is null")
        void adminCanGetAllPostsWithNullWall() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/by-wall")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            
            List<Map> posts = (List<Map>) response.body().get("data");
            assertTrue(posts.size() >= 4); // At least 4 posts total
        }

        @Test
        @DisplayName("Positive: Admin can sort national posts by MOST_LIKED")
        void adminCanSortNationalPostsByMostLiked() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/by-wall?wall=national&sortBy=MOST_LIKED")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> posts = (List<Map>) response.body().get("data");
            assertTrue(posts.size() >= 2);
            
            // Verify sorting (first post should have more likes than second)
            if (posts.size() >= 2) {
                int firstLikes = (int) posts.get(0).get("likeCount");
                int secondLikes = (int) posts.get(1).get("likeCount");
                assertTrue(firstLikes >= secondLikes);
            }
        }

        @Test
        @DisplayName("Positive: Admin can sort posts by OLDEST")
        void adminCanSortPostsByOldest() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/by-wall?wall=national&sortBy=OLDEST")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Admin can sort posts by LEAST_LIKED")
        void adminCanSortPostsByLeastLiked() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/by-wall?wall=campus&sortBy=LEAST_LIKED")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Pagination works with wall filtering")
        void paginationWorksWithWallFiltering() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/by-wall?wall=national&page=1&limit=1")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            Map pagination = (Map) response.body().get("pagination");
            assertEquals(1, pagination.get("page"));
            assertEquals(1, pagination.get("limit"));
            
            List<Map> posts = (List<Map>) response.body().get("data");
            assertTrue(posts.size() <= 1);
        }

        @Test
        @DisplayName("Negative: Regular user cannot access by-wall endpoint")
        void regularUserCannotAccessByWallEndpoint() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/by-wall?wall=national")
                        .bearerAuth(userToken),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Unauthenticated user cannot access by-wall endpoint")
        void unauthenticatedUserCannotAccessByWallEndpoint() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/by-wall?wall=national"),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }
}
