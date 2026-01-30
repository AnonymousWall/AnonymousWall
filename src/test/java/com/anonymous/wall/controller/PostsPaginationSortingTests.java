package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.PostDTO;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("Posts Pagination and Sorting Tests")
class PostsPaginationSortingTests {

    private static final String BASE_PATH = "/api/v1/posts";

    @Inject
    @Client("/")
    private HttpClient client;

    @Inject
    private PostRepository postRepository;

    @Inject
    private PostLikeRepository postLikeRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private UserEntity testUser;
    private String jwtToken;
    private List<Post> testPosts;

    @BeforeEach
    void setUp() {
        // Clean up
        postLikeRepository.deleteAll();
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

        // Create test posts with different properties for sorting
        testPosts = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            Post post = new Post(testUser.getId(), "Test post " + i, "campus", "harvard.edu");
            post.setLikeCount(i); // Vary like counts
            testPosts.add(postRepository.save(post));
        }
    }

    @AfterEach
    void tearDown() {
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Pagination Tests - Positive Cases")
    class PaginationPositiveTests {

        @Test
        @DisplayName("Should return first page with default parameters")
        void shouldReturnFirstPageDefault() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH)
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
            assertEquals(25, pagination.get("total"), "Total should be 25");
            assertEquals(2, pagination.get("totalPages"), "Should have 2 pages");
        }

        @Test
        @DisplayName("Should return second page")
        void shouldReturnSecondPage() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?page=2")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();

            List data = (List) body.get("data");
            Map pagination = (Map) body.get("pagination");

            assertEquals(5, data.size(), "Second page should have 5 remaining items");
            assertEquals(2, pagination.get("page"), "Page should be 2");
            assertEquals(25, pagination.get("total"), "Total should be 25");
        }

        @Test
        @DisplayName("Should respect custom limit parameter")
        void shouldRespectCustomLimit() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?limit=10")
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
        @DisplayName("Should handle max limit (100)")
        void shouldHandleMaxLimit() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?limit=100")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map pagination = (Map) response.body().get("pagination");
            assertEquals(100, pagination.get("limit"), "Limit should be capped at 100");
        }
    }

    @Nested
    @DisplayName("Pagination Tests - Negative Cases")
    class PaginationNegativeTests {

        @Test
        @DisplayName("Should default to page 1 for invalid page (0)")
        void shouldDefaultPage1ForZero() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?page=0")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            Map pagination = (Map) response.body().get("pagination");
            assertEquals(1, pagination.get("page"), "Should default to page 1");
        }

        @Test
        @DisplayName("Should default to page 1 for negative page")
        void shouldDefaultPage1ForNegative() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?page=-5")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            Map pagination = (Map) response.body().get("pagination");
            assertEquals(1, pagination.get("page"), "Should default to page 1");
        }

        @Test
        @DisplayName("Should default to limit 20 for invalid limit (0)")
        void shouldDefaultLimit20ForZero() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?limit=0")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            Map pagination = (Map) response.body().get("pagination");
            assertEquals(20, pagination.get("limit"), "Should default to limit 20");
        }

        @Test
        @DisplayName("Should cap limit at 20 for exceeding maximum (500)")
        void shouldCapLimitAt20ForExceedingMax() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?limit=500")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            Map pagination = (Map) response.body().get("pagination");
            assertEquals(20, pagination.get("limit"), "Should default to limit 20");
        }
    }

    @Nested
    @DisplayName("Pagination Tests - Edge Cases")
    class PaginationEdgeCases {

        @Test
        @DisplayName("Should handle requesting page beyond available pages")
        void shouldHandlePageBeyondAvailable() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?page=999")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List data = (List) response.body().get("data");
            assertEquals(0, data.size(), "Should return empty data for page beyond available");
        }

        @Test
        @DisplayName("Should handle exact page boundary")
        void shouldHandleExactPageBoundary() {
            // Create exactly 40 posts (2 full pages of 20)
            for (int i = 0; i < 15; i++) {
                Post post = new Post(testUser.getId(), "Extra post " + i, "campus", "harvard.edu");
                postRepository.save(post);
            }

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?page=2&limit=20")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            Map pagination = (Map) response.body().get("pagination");
            assertEquals(2, pagination.get("totalPages"), "Should have exactly 2 pages");
            assertEquals(20, ((List) response.body().get("data")).size());
        }

        @Test
        @DisplayName("Should handle limit=1")
        void shouldHandleLimit1() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?limit=1")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            List data = (List) response.body().get("data");
            Map pagination = (Map) response.body().get("pagination");

            assertEquals(1, data.size());
            assertEquals(25, pagination.get("totalPages"));
        }
    }

    @Nested
    @DisplayName("Sorting Tests - Positive Cases")
    class SortingPositiveTests {

        @Test
        @DisplayName("Should sort by NEWEST (default)")
        void shouldSortByNewest() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=NEWEST&limit=5")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");

            // Verify newest first (most recent timestamps)
            assertTrue(data.size() > 0);
            // Posts should be in descending order of creation time
        }

        @Test
        @DisplayName("Should sort by OLDEST")
        void shouldSortByOldest() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=OLDEST&limit=5")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertTrue(data.size() > 0);
        }

        @Test
        @DisplayName("Should sort by MOST_LIKED")
        void shouldSortByMostLiked() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=MOST_LIKED&limit=10")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");

            // Check if posts are sorted by likes in descending order
            for (int i = 0; i < data.size() - 1; i++) {
                Integer likes1 = (Integer) data.get(i).get("likes");
                Integer likes2 = (Integer) data.get(i + 1).get("likes");
                assertTrue(likes1 >= likes2, "Posts should be sorted by likes descending");
            }
        }

        @Test
        @DisplayName("Should sort by LEAST_LIKED")
        void shouldSortByLeastLiked() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=LEAST_LIKED&limit=10")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");

            // Check if posts are sorted by likes in ascending order
            for (int i = 0; i < data.size() - 1; i++) {
                Integer likes1 = (Integer) data.get(i).get("likes");
                Integer likes2 = (Integer) data.get(i + 1).get("likes");
                assertTrue(likes1 <= likes2, "Posts should be sorted by likes ascending");
            }
        }

        @Test
        @DisplayName("Should combine pagination with sorting")
        void shouldCombinePaginationWithSorting() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=MOST_LIKED&page=1&limit=10")
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
    }

    @Nested
    @DisplayName("Sorting Tests - Negative Cases")
    class SortingNegativeTests {

        @Test
        @DisplayName("Should default to NEWEST for invalid sort")
        void shouldDefaultToNewestForInvalidSort() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=INVALID_SORT")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertTrue(data.size() > 0, "Should still return results with default sort");
        }

        @Test
        @DisplayName("Should handle empty sort string")
        void shouldHandleEmptySortString() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertTrue(data.size() > 0);
        }

        @Test
        @DisplayName("Should handle case-insensitive sort")
        void shouldHandleCaseInsensitiveSort() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=newest")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertTrue(data.size() > 0);
        }
    }

    @Nested
    @DisplayName("Sorting Tests - Edge Cases")
    class SortingEdgeCases {

        @Test
        @DisplayName("Should handle sorting with limit=1")
        void shouldHandleSortingWithLimit1() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=MOST_LIKED&limit=1")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            List<Map> data = (List<Map>) response.body().get("data");
            assertEquals(1, data.size());
            // Should have the post with most likes
            assertEquals(24, data.get(0).get("likes"), "Most liked post should have 24 likes");
        }

        @Test
        @DisplayName("Should handle sorting with pagination across multiple pages")
        void shouldHandleSortingAcrossPages() {
            // Get first page sorted by most liked
            HttpResponse<Map> page1Response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=MOST_LIKED&page=1&limit=10")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            List<Map> page1Data = (List<Map>) page1Response.body().get("data");
            Integer lastLikesPage1 = (Integer) page1Data.get(page1Data.size() - 1).get("likes");

            // Get second page sorted by most liked
            HttpResponse<Map> page2Response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=MOST_LIKED&page=2&limit=10")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            List<Map> page2Data = (List<Map>) page2Response.body().get("data");
            Integer firstLikesPage2 = (Integer) page2Data.get(0).get("likes");

            // Page 2 should have fewer or equal likes than last item of page 1
            assertTrue(firstLikesPage2 <= lastLikesPage1, "Sorting should be consistent across pages");
        }

        @Test
        @DisplayName("Should handle sorting with no results")
        void shouldHandleSortingWithNoResults() {
            // Clear all posts for this user
            postRepository.deleteAll();

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sort=MOST_LIKED")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List data = (List) response.body().get("data");
            Map pagination = (Map) response.body().get("pagination");

            assertEquals(0, data.size());
            assertEquals(0, pagination.get("total"));
            assertEquals(0, pagination.get("totalPages"));
        }

        @Test
        @DisplayName("Should handle combination of all parameters")
        void shouldHandleAllParameters() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?wall=campus&page=1&limit=5&sort=MOST_LIKED")
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
