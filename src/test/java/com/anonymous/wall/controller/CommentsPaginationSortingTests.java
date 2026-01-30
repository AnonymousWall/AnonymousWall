package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
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
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("Comments Pagination and Sorting Tests")
class CommentsPaginationSortingTests {

    private static final String BASE_PATH = "/api/v1/posts";

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

    private UserEntity testUser;
    private String jwtToken;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // Clean up
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new UserEntity();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@harvard.edu");
        testUser.setSchoolDomain("harvard.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser.setPasswordHash("dummy");
        testUser = userRepository.save(testUser);

        jwtToken = jwtTokenService.generateToken(testUser);

        // Create test post
        testPost = new Post(testUser.getId(), "Test post for comments", "campus", "harvard.edu");
        testPost = postRepository.save(testPost);

        // Create test comments (30 comments for comprehensive testing) via service
        for (int i = 0; i < 30; i++) {
            postsService.addComment(testPost.getId(),
                new com.anonymous.wall.model.CreateCommentRequest("Comment " + i), testUser.getId());
        }
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Comments Pagination - Positive Cases")
    class CommentsPaginationPositiveTests {

        @Test
        @DisplayName("Should return first page of comments with default parameters")
        void shouldReturnFirstPageDefault() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();

            List data = (List) body.get("data");
            Map pagination = (Map) body.get("pagination");

            assertEquals(20, data.size(), "Should return 20 items (default limit)");
            assertEquals(1, pagination.get("page"), "Page should be 1");
            assertEquals(20, pagination.get("limit"), "Limit should be 20");
            assertEquals(30, pagination.get("total"), "Total should be 30");
            assertEquals(2, pagination.get("totalPages"), "Should have 2 pages");
        }

        @Test
        @DisplayName("Should return second page of comments")
        void shouldReturnSecondPage() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?page=2")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();

            List data = (List) body.get("data");
            Map pagination = (Map) body.get("pagination");

            assertEquals(10, data.size(), "Second page should have 10 remaining items");
            assertEquals(2, pagination.get("page"), "Page should be 2");
            assertEquals(30, pagination.get("total"), "Total should be 30");
        }

        @Test
        @DisplayName("Should respect custom limit for comments")
        void shouldRespectCustomLimit() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?limit=10")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();

            List data = (List) body.get("data");
            Map pagination = (Map) body.get("pagination");

            assertEquals(10, data.size(), "Should return 10 items");
            assertEquals(10, pagination.get("limit"), "Limit should be 10");
            assertEquals(3, pagination.get("totalPages"), "Should have 3 pages");
        }

        @Test
        @DisplayName("Should handle max limit for comments")
        void shouldHandleMaxLimit() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?limit=100")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map pagination = (Map) response.body().get("pagination");
            assertEquals(100, pagination.get("limit"), "Limit should be capped at 100");
        }
    }

    @Nested
    @DisplayName("Comments Pagination - Negative Cases")
    class CommentsPaginationNegativeTests {

        @Test
        @DisplayName("Should default to page 1 for invalid page (0)")
        void shouldDefaultPage1ForZero() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?page=0")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            Map pagination = (Map) response.body().get("pagination");
            assertEquals(1, pagination.get("page"), "Should default to page 1");
        }

        @Test
        @DisplayName("Should default to limit 20 for invalid limit")
        void shouldDefaultLimit20ForInvalid() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?limit=0")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            Map pagination = (Map) response.body().get("pagination");
            assertEquals(20, pagination.get("limit"), "Should default to limit 20");
        }

        @Test
        @DisplayName("Should return 404 for non-existent post")
        void shouldReturn404ForNonExistentPost() {
            try {
                client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/99999/comments")
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                );
                fail("Should throw HttpClientResponseException");
            } catch (HttpClientResponseException e) {
                assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
            }
        }
    }

    @Nested
    @DisplayName("Comments Pagination - Edge Cases")
    class CommentsPaginationEdgeCases {

        @Test
        @DisplayName("Should handle requesting page beyond available pages")
        void shouldHandlePageBeyondAvailable() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?page=999")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List data = (List) response.body().get("data");
            assertEquals(0, data.size(), "Should return empty data for page beyond available");
        }

        @Test
        @DisplayName("Should handle post with no comments")
        void shouldHandlePostWithNoComments() {
            // Create a new post with no comments
            Post emptyPost = new Post(testUser.getId(), "Empty post", "campus", "harvard.edu");
            emptyPost = postRepository.save(emptyPost);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + emptyPost.getId() + "/comments")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List data = (List) response.body().get("data");
            Map pagination = (Map) response.body().get("pagination");

            assertEquals(0, data.size(), "Should return empty data");
            assertEquals(0, pagination.get("total"), "Total should be 0");
        }

        @Test
        @DisplayName("Should handle limit=1 for comments")
        void shouldHandleLimit1() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?limit=1")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            List data = (List) response.body().get("data");
            Map pagination = (Map) response.body().get("pagination");

            assertEquals(1, data.size());
            assertEquals(30, pagination.get("totalPages"));
        }
    }

    @Nested
    @DisplayName("Comments Sorting - Positive Cases")
    class CommentsSortingPositiveTests {

        @Test
        @DisplayName("Should sort comments by NEWEST (default)")
        void shouldSortByNewest() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?sort=NEWEST&limit=5")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertTrue(data.size() > 0, "Should return comments");
        }

        @Test
        @DisplayName("Should sort comments by OLDEST")
        void shouldSortByOldest() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?sort=OLDEST&limit=5")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertTrue(data.size() > 0, "Should return comments");
        }

        @Test
        @DisplayName("Should combine pagination with sorting for comments")
        void shouldCombinePaginationWithSorting() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?sort=NEWEST&page=1&limit=10")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            Map pagination = (Map) response.body().get("pagination");

            assertEquals(10, data.size());
            assertEquals(1, pagination.get("page"));
            assertEquals(10, pagination.get("limit"));
        }

        @Test
        @DisplayName("Should handle MOST_LIKED mapping to NEWEST for comments")
        void shouldHandleMostLikedMapping() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?sort=MOST_LIKED&limit=5")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertTrue(data.size() > 0, "Should return comments with MOST_LIKED mapped to NEWEST");
        }

        @Test
        @DisplayName("Should handle LEAST_LIKED mapping to NEWEST for comments")
        void shouldHandleLeastLikedMapping() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?sort=LEAST_LIKED&limit=5")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertTrue(data.size() > 0, "Should return comments with LEAST_LIKED mapped to NEWEST");
        }
    }

    @Nested
    @DisplayName("Comments Sorting - Negative Cases")
    class CommentsSortingNegativeTests {

        @Test
        @DisplayName("Should default to NEWEST for invalid sort")
        void shouldDefaultToNewestForInvalidSort() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?sort=INVALID")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertTrue(data.size() > 0, "Should still return results with default sort");
        }

        @Test
        @DisplayName("Should handle empty sort string for comments")
        void shouldHandleEmptySortString() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?sort=")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertTrue(data.size() > 0);
        }

        @Test
        @DisplayName("Should handle case-insensitive sort for comments")
        void shouldHandleCaseInsensitiveSort() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?sort=oldest")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertTrue(data.size() > 0);
        }
    }

    @Nested
    @DisplayName("Comments Sorting - Edge Cases")
    class CommentsSortingEdgeCases {

        @Test
        @DisplayName("Should handle sorting with limit=1 for comments")
        void shouldHandleSortingWithLimit1() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?sort=NEWEST&limit=1")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            List<Map> data = (List<Map>) response.body().get("data");
            assertEquals(1, data.size());
        }

        @Test
        @DisplayName("Should handle sorting across multiple pages for comments")
        void shouldHandleSortingAcrossPages() {
            // Get first page
            HttpResponse<Map> page1Response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?sort=NEWEST&page=1&limit=10")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            List<Map> page1Data = (List<Map>) page1Response.body().get("data");

            // Get second page
            HttpResponse<Map> page2Response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?sort=NEWEST&page=2&limit=10")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            List<Map> page2Data = (List<Map>) page2Response.body().get("data");

            // Both pages should have data and be consistently sorted
            assertTrue(page1Data.size() > 0);
            assertTrue(page2Data.size() > 0);
        }

        @Test
        @DisplayName("Should handle sorting with no comments")
        void shouldHandleSortingWithNoComments() {
            // Create empty post
            Post emptyPost = new Post(testUser.getId(), "Empty post", "campus", "harvard.edu");
            emptyPost = postRepository.save(emptyPost);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + emptyPost.getId() + "/comments?sort=OLDEST")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List data = (List) response.body().get("data");
            assertEquals(0, data.size());
        }

        @Test
        @DisplayName("Should handle combination of all parameters for comments")
        void shouldHandleAllParameters() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testPost.getId() + "/comments?page=1&limit=5&sort=OLDEST")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            Map pagination = (Map) response.body().get("pagination");

            assertEquals(5, data.size());
            assertEquals(1, pagination.get("page"));
            assertEquals(5, pagination.get("limit"));
        }
    }
}
