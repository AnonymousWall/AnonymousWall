package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("PostsService - Comments Pagination and Sorting Tests")
class PostsServiceCommentsPaginationSortingTests {

    @Inject
    private PostsService postsService;

    @Inject
    private PostRepository postRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUser;
    private Post testPost;
    private UUID userId;

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
        userId = testUser.getId();

        // Create test post
        testPost = new Post(userId, "Test post for comments", "campus", "harvard.edu");
        testPost = postRepository.save(testPost);

        // Create test comments (40 comments for comprehensive testing)
        for (int i = 0; i < 40; i++) {
            Comment comment = new Comment(testPost.getId(), userId, "Comment " + i);
            commentRepository.save(comment);
        }
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Comments Service Pagination - Positive Cases")
    class CommentsPaginationPositiveTests {

        @Test
        @DisplayName("Should return first page of comments with default parameters")
        void shouldReturnFirstPageDefault() {
            Pageable pageable = Pageable.from(0, 20);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable);

            assertEquals(20, result.getContent().size(), "Should return 20 items");
            assertEquals(40, result.getTotalSize(), "Total should be 40");
            assertEquals(2, result.getTotalPages(), "Should have 2 pages");
        }

        @Test
        @DisplayName("Should return second page of comments")
        void shouldReturnSecondPage() {
            Pageable pageable = Pageable.from(1, 20); // Page 2 (0-based)
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable);

            assertEquals(20, result.getContent().size(), "Second page should have 20 items");
            assertEquals(40, result.getTotalSize(), "Total should be 40");
        }

        @Test
        @DisplayName("Should respect custom limit for comments")
        void shouldRespectCustomLimit() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable);

            assertEquals(10, result.getContent().size());
            assertEquals(4, result.getTotalPages(), "Should have 4 pages with limit 10");
        }

        @Test
        @DisplayName("Should handle max limit for comments")
        void shouldHandleMaxLimit() {
            Pageable pageable = Pageable.from(0, 100);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable);

            assertEquals(40, result.getContent().size(), "Should return all 40 items");
            assertEquals(1, result.getTotalPages());
        }
    }

    @Nested
    @DisplayName("Comments Service Pagination - Negative Cases")
    class CommentsPaginationNegativeTests {

        @Test
        @DisplayName("Should handle page beyond available pages")
        void shouldHandlePageBeyondAvailable() {
            Pageable pageable = Pageable.from(10, 20);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable);

            assertEquals(0, result.getContent().size(), "Should return empty for page beyond available");
        }

        @Test
        @DisplayName("Should return empty for post with no comments")
        void shouldReturnEmptyForPostWithNoComments() {
            // Create post with no comments
            Post emptyPost = new Post(userId, "Empty post", "campus", "harvard.edu");
            emptyPost = postRepository.save(emptyPost);

            Pageable pageable = Pageable.from(0, 20);
            Page<Comment> result = postsService.getCommentsWithPagination(emptyPost.getId(), pageable);

            assertEquals(0, result.getContent().size());
            assertEquals(0, result.getTotalSize());
        }
    }

    @Nested
    @DisplayName("Comments Service Pagination - Edge Cases")
    class CommentsPaginationEdgeCases {

        @Test
        @DisplayName("Should handle limit=1 for comments")
        void shouldHandleLimit1() {
            Pageable pageable = Pageable.from(0, 1);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable);

            assertEquals(1, result.getContent().size());
            assertEquals(40, result.getTotalPages(), "Should have 40 pages with limit 1");
        }

        @Test
        @DisplayName("Should handle exact page boundary for comments")
        void shouldHandleExactPageBoundary() {
            // 40 comments, limit 20 = exactly 2 pages
            Pageable pageable = Pageable.from(1, 20);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable);

            assertEquals(20, result.getContent().size());
            assertEquals(2, result.getTotalPages());
        }
    }

    @Nested
    @DisplayName("Comments Service Sorting - Positive Cases")
    class CommentsSortingPositiveTests {

        @Test
        @DisplayName("Should sort comments by NEWEST (default)")
        void shouldSortByNewest() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable, SortBy.NEWEST);

            assertEquals(10, result.getContent().size());
            // Verify newest first by checking creation times are descending
            List<Comment> comments = result.getContent();
            for (int i = 0; i < comments.size() - 1; i++) {
                assertTrue(
                    comments.get(i).getCreatedAt().isAfter(comments.get(i + 1).getCreatedAt()) ||
                    comments.get(i).getCreatedAt().isEqual(comments.get(i + 1).getCreatedAt()),
                    "Comments should be sorted by creation time descending"
                );
            }
        }

        @Test
        @DisplayName("Should sort comments by OLDEST")
        void shouldSortByOldest() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable, SortBy.OLDEST);

            assertEquals(10, result.getContent().size());
            // Verify oldest first by checking creation times are ascending
            List<Comment> comments = result.getContent();
            for (int i = 0; i < comments.size() - 1; i++) {
                assertTrue(
                    comments.get(i).getCreatedAt().isBefore(comments.get(i + 1).getCreatedAt()) ||
                    comments.get(i).getCreatedAt().isEqual(comments.get(i + 1).getCreatedAt()),
                    "Comments should be sorted by creation time ascending"
                );
            }
        }

        @Test
        @DisplayName("Should map MOST_LIKED to NEWEST for comments")
        void shouldMapMostLikedToNewest() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable, SortBy.MOST_LIKED);

            assertEquals(10, result.getContent().size());
            // Should be sorted by NEWEST (since MOST_LIKED maps to NEWEST)
            List<Comment> comments = result.getContent();
            for (int i = 0; i < comments.size() - 1; i++) {
                assertTrue(
                    comments.get(i).getCreatedAt().isAfter(comments.get(i + 1).getCreatedAt()) ||
                    comments.get(i).getCreatedAt().isEqual(comments.get(i + 1).getCreatedAt()),
                    "MOST_LIKED should map to NEWEST (descending creation time)"
                );
            }
        }

        @Test
        @DisplayName("Should map LEAST_LIKED to NEWEST for comments")
        void shouldMapLeastLikedToNewest() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable, SortBy.LEAST_LIKED);

            assertEquals(10, result.getContent().size());
            // Should be sorted by NEWEST (since LEAST_LIKED maps to NEWEST)
            List<Comment> comments = result.getContent();
            for (int i = 0; i < comments.size() - 1; i++) {
                assertTrue(
                    comments.get(i).getCreatedAt().isAfter(comments.get(i + 1).getCreatedAt()) ||
                    comments.get(i).getCreatedAt().isEqual(comments.get(i + 1).getCreatedAt()),
                    "LEAST_LIKED should map to NEWEST (descending creation time)"
                );
            }
        }
    }

    @Nested
    @DisplayName("Comments Service Sorting - Negative Cases")
    class CommentsSortingNegativeTests {

        @Test
        @DisplayName("Should handle null SortBy with default")
        void shouldHandleNullSortBy() {
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable, null);

            assertNotNull(result, "Should return results even with null SortBy");
            assertEquals(10, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("Comments Service Sorting - Edge Cases")
    class CommentsSortingEdgeCases {

        @Test
        @DisplayName("Should handle sorting with limit=1")
        void shouldHandleSortingWithLimit1() {
            Pageable pageable = Pageable.from(0, 1);
            Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable, SortBy.NEWEST);

            assertEquals(1, result.getContent().size());
            assertEquals(40, result.getTotalPages());
        }

        @Test
        @DisplayName("Should maintain sorting consistency across pages")
        void shouldMaintainSortingConsistencyAcrossPages() {
            // Get first page
            Pageable pageable1 = Pageable.from(0, 10);
            Page<Comment> page1 = postsService.getCommentsWithPagination(testPost.getId(), pageable1, SortBy.NEWEST);

            // Get second page
            Pageable pageable2 = Pageable.from(1, 10);
            Page<Comment> page2 = postsService.getCommentsWithPagination(testPost.getId(), pageable2, SortBy.NEWEST);

            // Last comment of page 1 should be older than first comment of page 2
            Comment lastPage1 = page1.getContent().get(page1.getContent().size() - 1);
            Comment firstPage2 = page2.getContent().get(0);

            assertTrue(
                lastPage1.getCreatedAt().isBefore(firstPage2.getCreatedAt()) ||
                lastPage1.getCreatedAt().isEqual(firstPage2.getCreatedAt()),
                "Sorting should be consistent: page1 last should be <= page2 first"
            );
        }

        @Test
        @DisplayName("Should handle combination of pagination and sorting for comments")
        void shouldHandlePaginationWithSorting() {
            Pageable pageable1 = Pageable.from(0, 15);
            Page<Comment> page1 = postsService.getCommentsWithPagination(testPost.getId(), pageable1, SortBy.OLDEST);

            assertEquals(15, page1.getContent().size());
            assertEquals(3, page1.getTotalPages(), "Should have 3 pages with 40 comments and limit 15");

            // Verify sorting within page
            List<Comment> comments = page1.getContent();
            for (int i = 0; i < comments.size() - 1; i++) {
                assertTrue(
                    comments.get(i).getCreatedAt().isBefore(comments.get(i + 1).getCreatedAt()) ||
                    comments.get(i).getCreatedAt().isEqual(comments.get(i + 1).getCreatedAt()),
                    "Page should maintain sort order"
                );
            }
        }
    }

    @Nested
    @DisplayName("Comments Service Integration Tests")
    class CommentsIntegrationTests {

        @Test
        @DisplayName("Should handle all SortBy options for comments")
        void shouldHandleAllSortByOptions() {
            Pageable pageable = Pageable.from(0, 10);

            for (SortBy sortBy : SortBy.values()) {
                Page<Comment> result = postsService.getCommentsWithPagination(testPost.getId(), pageable, sortBy);

                assertNotNull(result, "Should return result for " + sortBy);
                assertEquals(10, result.getContent().size(), "Should have 10 comments for " + sortBy);
            }
        }

        @Test
        @DisplayName("Should return same total regardless of sorting for comments")
        void shouldReturnSameTotalRegardlessOfSorting() {
            Pageable pageable = Pageable.from(0, 20);

            Page<Comment> newestResult = postsService.getCommentsWithPagination(testPost.getId(), pageable, SortBy.NEWEST);
            Page<Comment> oldestResult = postsService.getCommentsWithPagination(testPost.getId(), pageable, SortBy.OLDEST);

            assertEquals(newestResult.getTotalSize(), oldestResult.getTotalSize(),
                "Total should be same regardless of sort");
            assertEquals(40, newestResult.getTotalSize());
            assertEquals(40, oldestResult.getTotalSize());
        }

        @Test
        @DisplayName("Should return empty comments with pagination and sorting")
        void shouldReturnEmptyCommentsWithPaginationAndSorting() {
            Post emptyPost = new Post(userId, "Empty post", "campus", "harvard.edu");
            emptyPost = postRepository.save(emptyPost);

            Pageable pageable = Pageable.from(0, 20);
            Page<Comment> result = postsService.getCommentsWithPagination(emptyPost.getId(), pageable, SortBy.NEWEST);

            assertEquals(0, result.getContent().size());
            assertEquals(0, result.getTotalSize());
        }
    }
}
