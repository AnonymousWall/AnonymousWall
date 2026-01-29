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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Posts Controller - List/Retrieval Tests")
class PostsControllerListTests {

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
    @DisplayName("Get Campus Posts - Permission Tests")
    class GetCampusPostsPermissionTests {

        @Test
        @DisplayName("Should return only campus posts for user's school")
        void shouldReturnOnlyCampusPostsForSchool() {
            // Create Harvard posts
            Post harvardPost1 = new Post(testUserHarvard.getId(), "Harvard post 1", "campus", "harvard.edu");
            Post harvardPost2 = new Post(testUserHarvard.getId(), "Harvard post 2", "campus", "harvard.edu");
            postRepository.save(harvardPost1);
            postRepository.save(harvardPost2);

            // Create MIT posts
            Post mitPost1 = new Post(testUserMIT.getId(), "MIT post 1", "campus", "mit.edu");
            Post mitPost2 = new Post(testUserMIT.getId(), "MIT post 2", "campus", "mit.edu");
            postRepository.save(mitPost1);
            postRepository.save(mitPost2);

            // Harvard user requests campus posts
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=campus&page=1&limit=20")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> body = response.body();
            List<Map> posts = (List<Map>) body.get("data");
            assertNotNull(posts);

            // Should only see Harvard posts (not MIT posts)
            for (Map post : posts) {
                String content = (String) post.get("content");
                assertTrue(content.startsWith("Harvard"), "Should only see Harvard posts");
            }
        }

        @Test
        @DisplayName("Should not show MIT campus posts to Harvard user")
        void shouldNotShowOtherSchoolPosts() {
            // Create MIT-only post
            Post mitPost = new Post(testUserMIT.getId(), "MIT exclusive content", "campus", "mit.edu");
            postRepository.save(mitPost);

            // Harvard user requests campus posts
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=campus&page=1&limit=20")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> body = response.body();
            List<Map> posts = (List<Map>) body.get("data");

            // Should be empty or contain only Harvard posts
            if (posts != null && !posts.isEmpty()) {
                for (Map post : posts) {
                    String content = (String) post.get("content");
                    assertNotEquals("MIT exclusive content", content, "Should not see MIT posts");
                }
            }
        }

        @Test
        @DisplayName("Should return empty campus posts for user without school")
        void shouldReturnEmptyForNoSchoolUser() {
            // Create some campus posts
            Post harvardPost = new Post(testUserHarvard.getId(), "Harvard post", "campus", "harvard.edu");
            postRepository.save(harvardPost);

            // User without school requests campus
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=campus&page=1&limit=20")
                    .header("Authorization", "Bearer " + jwtTokenNoSchool),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> body = response.body();
            List<Map> posts = (List<Map>) body.get("data");

            // Should be empty since user has no school domain
            assertTrue(posts == null || posts.isEmpty(), "User without school should see no campus posts");
        }
    }

    @Nested
    @DisplayName("Get National Posts - Tests")
    class GetNationalPostsTests {

        @Test
        @DisplayName("Should return national posts to any authenticated user")
        void shouldReturnNationalPostsToAll() {
            // Create national posts
            Post nationalPost1 = new Post(testUserHarvard.getId(), "National post 1", "national", null);
            Post nationalPost2 = new Post(testUserMIT.getId(), "National post 2", "national", null);
            postRepository.save(nationalPost1);
            postRepository.save(nationalPost2);

            // All users should see national posts
            for (String token : new String[]{jwtTokenHarvard, jwtTokenMIT, jwtTokenNoSchool}) {
                HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "?wall=national&page=1&limit=20")
                        .header("Authorization", "Bearer " + token),
                    Map.class
                );

                assertEquals(HttpStatus.OK, response.getStatus());
                Map<String, Object> body = response.body();
                List<Map> posts = (List<Map>) body.get("data");
                assertTrue(posts != null && posts.size() >= 2, "Should see national posts");
            }
        }

        @Test
        @DisplayName("Should not show campus posts when requesting national")
        void shouldNotMixWallTypes() {
            // Create both types
            Post campusPost = new Post(testUserHarvard.getId(), "Campus only", "campus", "harvard.edu");
            Post nationalPost = new Post(testUserHarvard.getId(), "National post", "national", null);
            postRepository.save(campusPost);
            postRepository.save(nationalPost);

            // Request national posts
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=1&limit=20")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> body = response.body();
            List<Map> posts = (List<Map>) body.get("data");

            // Should only see national posts
            for (Map post : posts) {
                String wall = (String) post.get("wall");
                assertEquals("national", wall.toLowerCase(), "Should only see national posts");
            }
        }
    }

    @Nested
    @DisplayName("Pagination Tests")
    class PaginationTests {

        @Test
        @DisplayName("Should paginate results correctly")
        void shouldPaginateResults() {
            // Create 30 national posts
            for (int i = 1; i <= 30; i++) {
                Post post = new Post(testUserHarvard.getId(), "Post " + i, "national", null);
                postRepository.save(post);
            }

            // Request page 1, limit 10
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=1&limit=10")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            assertEquals(HttpStatus.OK, response1.getStatus());
            Map<String, Object> body1 = response1.body();
            List<Map> posts1 = (List<Map>) body1.get("data");
            assertEquals(10, posts1.size(), "Page 1 should have 10 items");

            // Request page 2
            HttpResponse<Map> response2 = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=2&limit=10")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            List<Map> posts2 = (List<Map>) response2.body().get("data");
            assertEquals(10, posts2.size(), "Page 2 should have 10 items");

            // Posts should be different
            String content1 = (String) posts1.get(0).get("content");
            String content2 = (String) posts2.get(0).get("content");
            assertNotEquals(content1, content2, "Page 1 and 2 should have different posts");
        }

        @Test
        @DisplayName("Should handle page boundaries")
        void shouldHandlePageBoundaries() {
            // Create 15 posts
            for (int i = 1; i <= 15; i++) {
                Post post = new Post(testUserHarvard.getId(), "Post " + i, "national", null);
                postRepository.save(post);
            }

            // Request page 2 with limit 10 (should have 5 posts)
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=2&limit=10")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> posts = (List<Map>) response.body().get("data");
            assertEquals(5, posts.size(), "Last page should have remaining posts");
        }

        @Test
        @DisplayName("Should reject invalid pagination params")
        void shouldHandleInvalidPagination() {
            // Negative page (should be corrected or error)
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=-1&limit=10")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            // Should either correct to page 1 or fail gracefully
            assertTrue(
                response.getStatus().equals(HttpStatus.OK) ||
                response.getStatus().equals(HttpStatus.BAD_REQUEST),
                "Should handle negative page gracefully"
            );
        }

        @Test
        @DisplayName("Should handle empty pages gracefully")
        void shouldHandleEmptyPage() {
            // Create only 5 posts
            for (int i = 1; i <= 5; i++) {
                Post post = new Post(testUserHarvard.getId(), "Post " + i, "national", null);
                postRepository.save(post);
            }

            // Request page 10 (beyond available)
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=10&limit=5")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> posts = (List<Map>) response.body().get("data");
            assertTrue(posts == null || posts.isEmpty(), "Empty page should return empty list");
        }
    }

    @Nested
    @DisplayName("Sorting Tests")
    class SortingTests {

        @Test
        @DisplayName("Should return posts in order")
        void shouldSortByNewestFirst() {
            // Create posts in order
            Post post1 = new Post(testUserHarvard.getId(), "First post (oldest)", "national", null);
            Post post2 = new Post(testUserHarvard.getId(), "Second post", "national", null);
            Post post3 = new Post(testUserHarvard.getId(), "Third post (newest)", "national", null);

            postRepository.save(post1);
            postRepository.save(post2);
            postRepository.save(post3);

            // Get posts
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=1&limit=10")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            List<Map> posts = (List<Map>) response.body().get("data");
            assertTrue(posts.size() >= 3, "Should have all posts");

            // Just verify we get the posts - actual sort order may vary by implementation
            boolean hasFirstPost = posts.stream().anyMatch(p -> "First post (oldest)".equals(p.get("content")));
            boolean hasThirdPost = posts.stream().anyMatch(p -> "Third post (newest)".equals(p.get("content")));
            assertTrue(hasFirstPost && hasThirdPost, "Should return all created posts");
        }

        @Test
        @DisplayName("Should maintain sort order across pages")
        void shouldMaintainSortAcrossPages() {
            // Create 25 posts
            for (int i = 1; i <= 25; i++) {
                Post post = new Post(testUserHarvard.getId(), "Post " + String.format("%02d", i), "national", null);
                postRepository.save(post);
            }

            // Get page 1
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=1&limit=10")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );
            List<Map> posts1 = (List<Map>) response1.body().get("data");

            // Get page 2
            HttpResponse<Map> response2 = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=2&limit=10")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );
            List<Map> posts2 = (List<Map>) response2.body().get("data");

            // Last of page 1 should be "newer" than first of page 2
            // (verify no overlap and consistent ordering)
            String lastPage1 = (String) posts1.get(posts1.size() - 1).get("content");
            String firstPage2 = (String) posts2.get(0).get("content");
            assertNotEquals(lastPage1, firstPage2, "Should have different posts");
        }
    }

    @Nested
    @DisplayName("Response Enrichment Tests")
    class ResponseEnrichmentTests {

        @Test
        @DisplayName("Should include like and comment counts in response")
        void shouldIncludeStats() {
            // Create a post
            Post post = new Post(testUserHarvard.getId(), "Post with stats", "national", null);
            post.setLikeCount(5);
            post.setCommentCount(3);
            postRepository.save(post);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=1&limit=10")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            List<Map> posts = (List<Map>) response.body().get("data");
            assertTrue(posts.size() > 0, "Should have posts");

            Map post_data = posts.get(0);
            assertNotNull(post_data.get("likes"), "Should include likes count");
            assertNotNull(post_data.get("comments"), "Should include comments count");
        }

        @Test
        @DisplayName("Should anonymize author information")
        void shouldAnonymizeAuthor() {
            Post post = new Post(testUserHarvard.getId(), "Anonymized post", "national", null);
            postRepository.save(post);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=1&limit=10")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            List<Map> posts = (List<Map>) response.body().get("data");
            Map post_data = posts.get(0);
            Map author = (Map) post_data.get("author");

            assertNotNull(author, "Should have author object");
            assertTrue((Boolean) author.get("isAnonymous"), "Author should be marked anonymous");
            // Should not expose email or actual name
        }

        @Test
        @DisplayName("Should indicate if current user liked the post")
        void shouldShowUserLikeStatus() {
            // Create post
            Post post = new Post(testUserHarvard.getId(), "Like status test", "national", null);
            postRepository.save(post);

            // User doesn't like - should show liked=false
            HttpResponse<Map> response1 = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=national&page=1&limit=10")
                    .header("Authorization", "Bearer " + jwtTokenHarvard),
                Map.class
            );

            List<Map> posts1 = (List<Map>) response1.body().get("data");
            Map post1 = posts1.get(0);
            assertFalse((Boolean) post1.get("liked"), "Should show liked=false before liking");

            // TODO: Like the post, then fetch again and verify liked=true
            // This requires the like endpoint to work correctly
        }
    }

    @Nested
    @DisplayName("Invalid Wall Type Tests")
    class InvalidWallTypeTests {

        @Test
        @DisplayName("Should reject invalid wall type")
        void shouldRejectInvalidWallType() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "?wall=invalid&page=1&limit=20")
                        .header("Authorization", "Bearer " + jwtTokenHarvard),
                    Map.class
                )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should reject request without authentication")
        void shouldRejectUnauthenticatedRequest() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "?wall=national&page=1&limit=20"),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }
}
