package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import com.anonymous.wall.service.PostsService;
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
@DisplayName("UserController Posts Tests")
class UserControllerPostsTest {

    private static final String BASE_PATH = "/api/v1/users/me/posts";

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
    private PostsService postsService;

    private UserEntity testUser1;
    private UserEntity testUser2;
    private String jwtToken1;
    private String jwtToken2;

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
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("User Own Posts - Basic Functionality")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Should return user's own posts")
        void shouldReturnUserOwnPosts() {
            // User1 creates 2 posts
            postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 1", "Content 1"), 
                null, testUser1.getId());
            postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 2", "Content 2"), 
                null, testUser1.getId());

            // User2 creates 1 post
            postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 3", "Content 3"), 
                null, testUser2.getId());

            String endpoint = BASE_PATH;
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatus());
            
            Map<String, Object> body = response.body();
            assertNotNull(body);
            
            List<Map> data = (List<Map>) body.get("data");
            assertNotNull(data);
            assertEquals(2, data.size(), "User1 should have 2 posts");

            // Verify posts belong to user1
            for (Map post : data) {
                Map author = (Map) post.get("author");
                assertEquals(testUser1.getId().toString(), author.get("id"));
            }
        }

        @Test
        @DisplayName("Should return empty list when user has no posts")
        void shouldReturnEmptyListWhenNoPosts() {
            // User1 has no posts
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
        @DisplayName("Should only return user's own posts, not others")
        void shouldOnlyReturnOwnPosts() {
            // User1 creates 3 posts
            for (int i = 0; i < 3; i++) {
                postsService.createPost(
                    new com.anonymous.wall.model.CreatePostRequest("User1 Title " + i, "User1 Content " + i), 
                    null, testUser1.getId());
            }

            // User2 creates 5 posts
            for (int i = 0; i < 5; i++) {
                postsService.createPost(
                    new com.anonymous.wall.model.CreatePostRequest("User2 Title " + i, "User2 Content " + i), 
                    null, testUser2.getId());
            }

            // User1 should only see their 3 posts
            String endpoint = BASE_PATH;
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(3, data.size());

            // User2 should only see their 5 posts
            request = HttpRequest.GET(endpoint).bearerAuth(jwtToken2);
            response = client.toBlocking().exchange(request, Map.class);

            body = response.body();
            data = (List<Map>) body.get("data");
            assertEquals(5, data.size());
        }
    }

    @Nested
    @DisplayName("User Own Posts - Pagination")
    class PaginationTests {

        @Test
        @DisplayName("Should paginate posts correctly")
        void shouldPaginatePostsCorrectly() {
            // Create 25 posts for user1
            for (int i = 0; i < 25; i++) {
                postsService.createPost(
                    new com.anonymous.wall.model.CreatePostRequest("Title " + i, "Content " + i), 
                    null, testUser1.getId());
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
            // Create 15 posts
            for (int i = 0; i < 15; i++) {
                postsService.createPost(
                    new com.anonymous.wall.model.CreatePostRequest("Title " + i, "Content " + i), 
                    null, testUser1.getId());
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
    @DisplayName("User Own Posts - Sorting")
    class SortingTests {

        @Test
        @DisplayName("Should sort posts by newest first (default)")
        void shouldSortByNewestFirst() throws InterruptedException {
            // Create posts with delays to ensure different timestamps
            for (int i = 1; i <= 3; i++) {
                postsService.createPost(
                    new com.anonymous.wall.model.CreatePostRequest("Title " + i, "Content " + i), 
                    null, testUser1.getId());
                Thread.sleep(1000); // 1 second delay to ensure different timestamps
            }

            String endpoint = BASE_PATH + "?sort=NEWEST";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(3, data.size());

            // Verify newest first (Title 3, Title 2, Title 1)
            assertEquals("Title 3", data.get(0).get("title"));
            assertEquals("Title 2", data.get(1).get("title"));
            assertEquals("Title 1", data.get(2).get("title"));
        }

        @Test
        @DisplayName("Should sort posts by oldest first")
        void shouldSortByOldestFirst() throws InterruptedException {
            // Create posts with delays
            for (int i = 1; i <= 3; i++) {
                postsService.createPost(
                    new com.anonymous.wall.model.CreatePostRequest("Title " + i, "Content " + i), 
                    null, testUser1.getId());
                Thread.sleep(1000); // 1 second delay to ensure different timestamps
            }

            String endpoint = BASE_PATH + "?sort=OLDEST";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(3, data.size());

            // Verify oldest first (Title 1, Title 2, Title 3)
            assertEquals("Title 1", data.get(0).get("title"));
            assertEquals("Title 2", data.get(1).get("title"));
            assertEquals("Title 3", data.get(2).get("title"));
        }

        @Test
        @DisplayName("Should sort posts by most liked")
        void shouldSortByMostLiked() {
            // Create posts
            Post post1 = postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 1", "Content 1"), 
                null, testUser1.getId());
            Post post2 = postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 2", "Content 2"), 
                null, testUser1.getId());
            Post post3 = postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 3", "Content 3"), 
                null, testUser1.getId());

            // Add likes to simulate different like counts
            // Post2: 5 likes, Post3: 2 likes, Post1: 0 likes
            post2.setLikeCount(post2.getLikeCount() + 5);
            postRepository.update(post2);

            post3.setLikeCount(post3.getLikeCount() + 2);
            postRepository.update(post3);

            String endpoint = BASE_PATH + "?sort=MOST_LIKED";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(3, data.size());

            // Verify most liked first (Post2: 5, Post3: 2, Post1: 0)
            assertEquals("Title 2", data.get(0).get("title"));
            assertEquals(5, data.get(0).get("likes"));
            assertEquals("Title 3", data.get(1).get("title"));
            assertEquals(2, data.get(1).get("likes"));
            assertEquals("Title 1", data.get(2).get("title"));
            assertEquals(0, data.get(2).get("likes"));
        }

        @Test
        @DisplayName("Should sort posts by least liked")
        void shouldSortByLeastLiked() {
            // Create posts
            Post post1 = postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 1", "Content 1"), 
                null, testUser1.getId());
            Post post2 = postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 2", "Content 2"), 
                null, testUser1.getId());
            Post post3 = postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 3", "Content 3"), 
                null, testUser1.getId());

            // Add likes to simulate different like counts
            // Post2: 5 likes, Post3: 2 likes, Post1: 0 likes
            post2.setLikeCount(post2.getLikeCount() + 5);
            postRepository.update(post2);

            post3.setLikeCount(post3.getLikeCount() + 2);
            postRepository.update(post3);

            String endpoint = BASE_PATH + "?sort=LEAST_LIKED";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(3, data.size());

            // Verify least liked first (Post1: 0, Post3: 2, Post2: 5)
            assertEquals("Title 1", data.get(0).get("title"));
            assertEquals(0, data.get(0).get("likes"));
            assertEquals("Title 3", data.get(1).get("title"));
            assertEquals(2, data.get(1).get("likes"));
            assertEquals("Title 2", data.get(2).get("title"));
            assertEquals(5, data.get(2).get("likes"));
        }
    }

    @Nested
    @DisplayName("User Own Posts - Hidden Posts")
    class HiddenPostsTests {

        @Test
        @DisplayName("Should exclude hidden posts")
        void shouldExcludeHiddenPostsByDefault() {
            // Create 3 posts
            Post post1 = postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 1", "Content 1"), 
                null, testUser1.getId());
            Post post2 = postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 2", "Content 2"), 
                null, testUser1.getId());
            postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 3", "Content 3"), 
                null, testUser1.getId());

            // Hide post2
            postsService.hidePost(post2.getId(), testUser1.getId());

            String endpoint = BASE_PATH;
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(2, data.size(), "Should only return non-hidden posts");

            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(2, pagination.get("total"));
        }
    }

    @Nested
    @DisplayName("User Own Posts - Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle invalid page parameter")
        void shouldHandleInvalidPageParameter() {
            postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 1", "Content 1"), 
                null, testUser1.getId());

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
            postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 1", "Content 1"), 
                null, testUser1.getId());

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
            postsService.createPost(
                new com.anonymous.wall.model.CreatePostRequest("Title 1", "Content 1"), 
                null, testUser1.getId());

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
    @DisplayName("User Own Posts - Performance")
    class PerformanceTests {

        @Test
        @DisplayName("Should efficiently retrieve large number of posts")
        void shouldEfficientlyRetrieveLargeNumberOfPosts() {
            // Create 100 posts
            for (int i = 0; i < 100; i++) {
                postsService.createPost(
                    new com.anonymous.wall.model.CreatePostRequest("Title " + i, "Content " + i), 
                    null, testUser1.getId());
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
}
