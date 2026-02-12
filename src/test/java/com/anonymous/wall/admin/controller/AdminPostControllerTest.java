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

        @Test
        @DisplayName("Positive: Filter posts by wall type - campus")
        void filterPostsByWallCampus() {
            // Create campus and national posts
            Post campusPost = new Post();
            campusPost.setUserId(regularUser.getId());
            campusPost.setTitle("Campus Post");
            campusPost.setContent("Campus content");
            campusPost.setWall("campus");
            campusPost.setSchoolDomain("test.edu");
            postRepository.save(campusPost);

            Post nationalPost = new Post();
            nationalPost.setUserId(regularUser.getId());
            nationalPost.setTitle("National Post");
            nationalPost.setContent("National content");
            nationalPost.setWall("national");
            nationalPost.setSchoolDomain("test.edu");
            postRepository.save(nationalPost);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=campus")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            
            List<Map> posts = (List<Map>) response.body().get("data");
            // All returned posts should be campus posts
            for (Map post : posts) {
                assertEquals("campus", post.get("wall"));
            }
        }

        @Test
        @DisplayName("Positive: Filter posts by wall type - national")
        void filterPostsByWallNational() {
            // Create campus and national posts
            Post campusPost = new Post();
            campusPost.setUserId(regularUser.getId());
            campusPost.setTitle("Campus Post");
            campusPost.setContent("Campus content");
            campusPost.setWall("campus");
            campusPost.setSchoolDomain("test.edu");
            postRepository.save(campusPost);

            Post nationalPost = new Post();
            nationalPost.setUserId(regularUser.getId());
            nationalPost.setTitle("National Post");
            nationalPost.setContent("National content");
            nationalPost.setWall("national");
            nationalPost.setSchoolDomain("test.edu");
            postRepository.save(nationalPost);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            
            List<Map> posts = (List<Map>) response.body().get("data");
            // All returned posts should be national posts
            for (Map post : posts) {
                assertEquals("national", post.get("wall"));
            }
        }

        @Test
        @DisplayName("Positive: Combine wall filter with other filters")
        void combineWallFilterWithOtherFilters() {
            // Create posts with different combinations
            Post hiddenCampusPost = new Post();
            hiddenCampusPost.setUserId(regularUser.getId());
            hiddenCampusPost.setTitle("Hidden Campus Post");
            hiddenCampusPost.setContent("Content");
            hiddenCampusPost.setWall("campus");
            hiddenCampusPost.setSchoolDomain("test.edu");
            hiddenCampusPost.setHidden(true);
            postRepository.save(hiddenCampusPost);

            Post visibleCampusPost = new Post();
            visibleCampusPost.setUserId(regularUser.getId());
            visibleCampusPost.setTitle("Visible Campus Post");
            visibleCampusPost.setContent("Content");
            visibleCampusPost.setWall("campus");
            visibleCampusPost.setSchoolDomain("test.edu");
            visibleCampusPost.setHidden(false);
            postRepository.save(visibleCampusPost);

            Post visibleNationalPost = new Post();
            visibleNationalPost.setUserId(regularUser.getId());
            visibleNationalPost.setTitle("Visible National Post");
            visibleNationalPost.setContent("Content");
            visibleNationalPost.setWall("national");
            visibleNationalPost.setSchoolDomain("test.edu");
            visibleNationalPost.setHidden(false);
            postRepository.save(visibleNationalPost);

            // Act - filter by campus and not hidden
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=campus&hidden=false")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            
            List<Map> posts = (List<Map>) response.body().get("data");
            // All returned posts should be campus and not hidden
            for (Map post : posts) {
                assertEquals("campus", post.get("wall"));
                assertFalse((Boolean) post.get("hidden"));
            }
        }

        @Test
        @DisplayName("Positive: Filter by wall, userId, and hidden status")
        void filterByWallUserIdAndHidden() {
            // Create posts with different combinations
            Post campusPost = new Post();
            campusPost.setUserId(regularUser.getId());
            campusPost.setTitle("Campus Post by Regular User");
            campusPost.setContent("Content");
            campusPost.setWall("campus");
            campusPost.setSchoolDomain("test.edu");
            campusPost.setHidden(false);
            postRepository.save(campusPost);

            Post nationalPost = new Post();
            nationalPost.setUserId(adminUser.getId());
            nationalPost.setTitle("National Post by Admin");
            nationalPost.setContent("Content");
            nationalPost.setWall("national");
            nationalPost.setSchoolDomain("test.edu");
            nationalPost.setHidden(false);
            postRepository.save(nationalPost);

            // Act - filter by campus, specific user, and not hidden
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=campus&userId=" + regularUser.getId() + "&hidden=false")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            
            List<Map> posts = (List<Map>) response.body().get("data");
            // All returned posts should match all three filters
            for (Map post : posts) {
                assertEquals("campus", post.get("wall"));
                assertEquals(regularUser.getId().toString(), post.get("userId"));
                assertFalse((Boolean) post.get("hidden"));
            }
        }
    }
}
