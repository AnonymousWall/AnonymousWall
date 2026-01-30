package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("PostsService - Pagination and Sorting Tests")
class PostsServicePaginationSortingTests {

    @Inject
    private PostsService postsService;

    @Inject
    private PostRepository postRepository;

    @Inject
    private PostLikeRepository postLikeRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUserCampus;
    private UserEntity testUserNational;
    private UUID campusUserId;
    private UUID nationalUserId;

    @BeforeEach
    void setUp() {
        // Clean up
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUserCampus = new UserEntity();
        testUserCampus.setId(UUID.randomUUID());
        testUserCampus.setEmail("campus@harvard.edu");
        testUserCampus.setSchoolDomain("harvard.edu");
        testUserCampus.setVerified(true);
        testUserCampus.setPasswordSet(true);
        testUserCampus.setPasswordHash("dummy");
        testUserCampus = userRepository.save(testUserCampus);
        campusUserId = testUserCampus.getId();

        testUserNational = new UserEntity();
        testUserNational.setId(UUID.randomUUID());
        testUserNational.setEmail("national@example.com");
        testUserNational.setSchoolDomain(null);
        testUserNational.setVerified(true);
        testUserNational.setPasswordSet(true);
        testUserNational.setPasswordHash("dummy");
        testUserNational = userRepository.save(testUserNational);
        nationalUserId = testUserNational.getId();

        // Create test posts with varying likes for sorting tests
        for (int i = 0; i < 35; i++) {
            Post post = new Post(campusUserId, "Campus post " + i, "campus", "harvard.edu");
            post.setLikeCount(i); // Vary like counts
            postRepository.save(post);
        }

        // Create national posts
        for (int i = 0; i < 25; i++) {
            Post post = new Post(nationalUserId, "National post " + i, "national", null);
            post.setLikeCount(i);
            postRepository.save(post);
        }
    }

    @AfterEach
    void tearDown() {
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Service Pagination - Campus Wall - Positive Cases")
    class ServicePaginationCampusPositiveTests {

        @Test
        @DisplayName("Should return first page of campus posts with default parameters")
        void shouldReturnFirstPageDefault() {
            Pageable pageable = Pageable.from(0, 20);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId);

            assertEquals(20, result.getContent().size(), "Should return 20 items");
            assertEquals(35, result.getTotalSize(), "Total should be 35");
            assertEquals(2, result.getTotalPages(), "Should have 2 pages");
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should return second page of campus posts")
        void shouldReturnSecondPage() {
            Pageable pageable = Pageable.from(1, 20); // Page 2 (0-based)
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId);

            assertEquals(15, result.getContent().size(), "Second page should have 15 items");
            assertEquals(35, result.getTotalSize(), "Total should be 35");
        }

        @Test
        @DisplayName("Should respect custom limit for campus posts")
        void shouldRespectCustomLimit() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId);

            assertEquals(10, result.getContent().size());
            assertEquals(4, result.getTotalPages(), "Should have 4 pages with limit 10");
        }

        @Test
        @DisplayName("Should return national posts with pagination")
        void shouldReturnNationalPostsWithPagination() {
            Pageable pageable = Pageable.from(0, 20);
            Page<Post> result = postsService.getPostsByWall("national", pageable, nationalUserId);

            assertEquals(20, result.getContent().size());
            assertEquals(25, result.getTotalSize(), "Total should be 25");
            assertEquals(2, result.getTotalPages());
        }
    }

    @Nested
    @DisplayName("Service Pagination - Negative Cases")
    class ServicePaginationNegativeTests {

        @Test
        @DisplayName("Should throw exception for invalid wall type")
        void shouldThrowExceptionForInvalidWallType() {
            Pageable pageable = Pageable.from(0, 20);

            assertThrows(IllegalArgumentException.class, () ->
                postsService.getPostsByWall("invalid_wall", pageable, campusUserId)
            );
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            Pageable pageable = Pageable.from(0, 20);
            UUID nonExistentUserId = UUID.randomUUID();

            assertThrows(IllegalArgumentException.class, () ->
                postsService.getPostsByWall("campus", pageable, nonExistentUserId)
            );
        }

        @Test
        @DisplayName("Should return empty for campus user without school domain requesting campus posts")
        void shouldReturnEmptyForUserWithoutSchoolDomain() {
            // Create user without school domain
            UserEntity userNoSchool = new UserEntity();
            userNoSchool.setId(UUID.randomUUID());
            userNoSchool.setEmail("noschool@example.com");
            userNoSchool.setSchoolDomain(null);
            userNoSchool.setVerified(true);
            userNoSchool.setPasswordSet(true);
            userNoSchool.setPasswordHash("dummy");
            userNoSchool = userRepository.save(userNoSchool);

            Pageable pageable = Pageable.from(0, 20);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userNoSchool.getId());

            assertTrue(result.isEmpty(), "Should return empty page for user without school domain");
        }
    }

    @Nested
    @DisplayName("Service Pagination - Edge Cases")
    class ServicePaginationEdgeCases {

        @Test
        @DisplayName("Should handle page beyond available pages")
        void shouldHandlePageBeyondAvailable() {
            Pageable pageable = Pageable.from(10, 20); // Way beyond
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId);

            assertEquals(0, result.getContent().size(), "Should return empty for page beyond available");
        }

        @Test
        @DisplayName("Should handle limit=1")
        void shouldHandleLimit1() {
            Pageable pageable = Pageable.from(0, 1);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId);

            assertEquals(1, result.getContent().size());
            assertEquals(35, result.getTotalPages(), "Should have 35 pages with limit 1");
        }
    }

    @Nested
    @DisplayName("Service Sorting - Positive Cases")
    class ServiceSortingPositiveTests {

        @Test
        @DisplayName("Should sort by NEWEST (default)")
        void shouldSortByNewest() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId, SortBy.NEWEST);

            assertEquals(10, result.getContent().size());
            // Verify newest first by checking creation times are descending
            List<Post> posts = result.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertTrue(
                    posts.get(i).getCreatedAt().isAfter(posts.get(i + 1).getCreatedAt()) ||
                    posts.get(i).getCreatedAt().isEqual(posts.get(i + 1).getCreatedAt()),
                    "Posts should be sorted by creation time descending"
                );
            }
        }

        @Test
        @DisplayName("Should sort by OLDEST")
        void shouldSortByOldest() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId, SortBy.OLDEST);

            assertEquals(10, result.getContent().size());
            // Verify oldest first by checking creation times are ascending
            List<Post> posts = result.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertTrue(
                    posts.get(i).getCreatedAt().isBefore(posts.get(i + 1).getCreatedAt()) ||
                    posts.get(i).getCreatedAt().isEqual(posts.get(i + 1).getCreatedAt()),
                    "Posts should be sorted by creation time ascending"
                );
            }
        }

        @Test
        @DisplayName("Should sort by MOST_LIKED")
        void shouldSortByMostLiked() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId, SortBy.MOST_LIKED);

            assertEquals(10, result.getContent().size());
            // Verify most liked first (descending likes)
            List<Post> posts = result.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertTrue(
                    posts.get(i).getLikeCount() >= posts.get(i + 1).getLikeCount(),
                    "Posts should be sorted by likes descending"
                );
            }
        }

        @Test
        @DisplayName("Should sort by LEAST_LIKED")
        void shouldSortByLeastLiked() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId, SortBy.LEAST_LIKED);

            assertEquals(10, result.getContent().size());
            // Verify least liked first (ascending likes)
            List<Post> posts = result.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertTrue(
                    posts.get(i).getLikeCount() <= posts.get(i + 1).getLikeCount(),
                    "Posts should be sorted by likes ascending"
                );
            }
        }

        @Test
        @DisplayName("Should apply sorting for national posts")
        void shouldApplySortingForNationalPosts() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("national", pageable, nationalUserId, SortBy.MOST_LIKED);

            assertEquals(10, result.getContent().size());
            List<Post> posts = result.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertTrue(
                    posts.get(i).getLikeCount() >= posts.get(i + 1).getLikeCount(),
                    "National posts should be sorted by likes descending"
                );
            }
        }
    }

    @Nested
    @DisplayName("Service Sorting - Negative Cases")
    class ServiceSortingNegativeTests {

        @Test
        @DisplayName("Should handle null SortBy with default")
        void shouldHandleNullSortBy() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId, null);

            assertNotNull(result, "Should return results even with null SortBy");
            assertEquals(10, result.getContent().size());
        }

        @Test
        @DisplayName("Should throw exception for invalid wall with sorting")
        void shouldThrowExceptionForInvalidWallWithSorting() {
            Pageable pageable = Pageable.from(0, 20);

            assertThrows(IllegalArgumentException.class, () ->
                postsService.getPostsByWall("invalid", pageable, campusUserId, SortBy.NEWEST)
            );
        }
    }

    @Nested
    @DisplayName("Service Sorting - Edge Cases")
    class ServiceSortingEdgeCases {

        @Test
        @DisplayName("Should handle sorting with limit=1")
        void shouldHandleSortingWithLimit1() {
            Pageable pageable = Pageable.from(0, 1);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId, SortBy.MOST_LIKED);

            assertEquals(1, result.getContent().size());
            // Should have the most liked post
            assertEquals(34, result.getContent().get(0).getLikeCount(), "Should have post with 34 likes (most liked)");
        }

        @Test
        @DisplayName("Should maintain sorting consistency across pages")
        void shouldMaintainSortingConsistencyAcrossPages() {
            // Get first page
            Pageable pageable1 = Pageable.from(0, 10);
            Page<Post> page1 = postsService.getPostsByWall("campus", pageable1, campusUserId, SortBy.MOST_LIKED);
            int lastLikesPage1 = page1.getContent().get(page1.getContent().size() - 1).getLikeCount();

            // Get second page
            Pageable pageable2 = Pageable.from(1, 10);
            Page<Post> page2 = postsService.getPostsByWall("campus", pageable2, campusUserId, SortBy.MOST_LIKED);
            int firstLikesPage2 = page2.getContent().get(0).getLikeCount();

            // Page 2 first item should have <= likes than page 1 last item
            assertTrue(
                firstLikesPage2 <= lastLikesPage1,
                "Sorting should be consistent across pages: page1 last (" + lastLikesPage1 +
                ") >= page2 first (" + firstLikesPage2 + ")"
            );
        }

        @Test
        @DisplayName("Should handle sorting with all posts having same likes")
        void shouldHandleSortingWithSameLikes() {
            // Create posts with same like count
            for (int i = 0; i < 10; i++) {
                Post post = new Post(campusUserId, "Same likes post " + i, "campus", "harvard.edu");
                post.setLikeCount(10); // All have 10 likes
                postRepository.save(post);
            }

            Pageable pageable = Pageable.from(0, 20);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId, SortBy.MOST_LIKED);

            assertNotNull(result);
            assertTrue(result.getContent().size() > 0, "Should return posts even with same like counts");
        }

        @Test
        @DisplayName("Should handle combination of pagination and sorting")
        void shouldHandlePaginationWithSorting() {
            Pageable pageable1 = Pageable.from(0, 15);
            Page<Post> page1 = postsService.getPostsByWall("campus", pageable1, campusUserId, SortBy.LEAST_LIKED);

            assertEquals(15, page1.getContent().size());
            assertEquals(3, page1.getTotalPages(), "Should have 3 pages with 35 posts and limit 15");

            // Verify sorting within page
            List<Post> posts = page1.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertTrue(
                    posts.get(i).getLikeCount() <= posts.get(i + 1).getLikeCount(),
                    "Page should maintain sort order"
                );
            }
        }
    }

    @Nested
    @DisplayName("Service Integration - Pagination and Sorting Combined")
    class ServiceIntegrationTests {

        @Test
        @DisplayName("Should combine pagination, sorting, and permission checks")
        void shouldCombineAllFeatures() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId, SortBy.MOST_LIKED);

            assertNotNull(result);
            assertEquals(10, result.getContent().size());
            assertEquals(35, result.getTotalSize());

            // Verify all posts are from correct wall
            result.getContent().forEach(post ->
                assertEquals("campus", post.getWall(), "All posts should be from campus wall")
            );

            // Verify sorting
            List<Post> posts = result.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertTrue(
                    posts.get(i).getLikeCount() >= posts.get(i + 1).getLikeCount(),
                    "Posts should be sorted by likes descending"
                );
            }
        }

        @Test
        @DisplayName("Should handle all SortBy options correctly")
        void shouldHandleAllSortByOptions() {
            Pageable pageable = Pageable.from(0, 10);

            // Test each sort option
            for (SortBy sortBy : SortBy.values()) {
                Page<Post> result = postsService.getPostsByWall("campus", pageable, campusUserId, sortBy);

                assertNotNull(result, "Should return result for " + sortBy);
                assertEquals(10, result.getContent().size(), "Should have 10 posts for " + sortBy);
            }
        }

        @Test
        @DisplayName("Should return same total regardless of sorting")
        void shouldReturnSameTotalRegardlessOfSorting() {
            Pageable pageable = Pageable.from(0, 20);

            Page<Post> newestResult = postsService.getPostsByWall("campus", pageable, campusUserId, SortBy.NEWEST);
            Page<Post> mostLikedResult = postsService.getPostsByWall("campus", pageable, campusUserId, SortBy.MOST_LIKED);

            assertEquals(newestResult.getTotalSize(), mostLikedResult.getTotalSize(),
                "Total should be same regardless of sort");
            assertEquals(35, newestResult.getTotalSize());
            assertEquals(35, mostLikedResult.getTotalSize());
        }
    }
}
