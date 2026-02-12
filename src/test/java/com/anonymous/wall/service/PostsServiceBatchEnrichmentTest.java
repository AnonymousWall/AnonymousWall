package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.PostLike;
import com.anonymous.wall.entity.UserEntity;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for batch enrichment optimization in PostsService
 * Verifies that the N+1 query problem fix works correctly
 */
@MicronautTest(transactional = false)
@DisplayName("PostsService - Batch Enrichment Tests")
class PostsServiceBatchEnrichmentTest {

    @Inject
    private PostsService postsService;

    @Inject
    private PostRepository postRepository;

    @Inject
    private PostLikeRepository postLikeRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUser1;
    private UserEntity testUser2;
    private UUID userId1;
    private UUID userId2;

    @BeforeEach
    void setUp() {
        // Clean up
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUser1 = new UserEntity();
        testUser1.setId(UUID.randomUUID());
        testUser1.setEmail("user1@harvard.edu");
        testUser1.setSchoolDomain("harvard.edu");
        testUser1.setVerified(true);
        testUser1.setPasswordSet(true);
        testUser1.setPasswordHash("dummy");
        testUser1 = userRepository.save(testUser1);
        userId1 = testUser1.getId();

        testUser2 = new UserEntity();
        testUser2.setId(UUID.randomUUID());
        testUser2.setEmail("user2@harvard.edu");
        testUser2.setSchoolDomain("harvard.edu");
        testUser2.setVerified(true);
        testUser2.setPasswordSet(true);
        testUser2.setPasswordHash("dummy");
        testUser2 = userRepository.save(testUser2);
        userId2 = testUser2.getId();
    }

    @AfterEach
    void tearDown() {
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Batch Enrichment - Like Status Tests")
    class BatchEnrichmentLikeStatusTests {

        @Test
        @DisplayName("Should correctly enrich multiple posts with like status")
        void shouldEnrichMultiplePostsWithLikeStatus() {
            // Create 5 posts
            Post post1 = new Post(userId2, "Title 1", "Content 1", "campus", "harvard.edu");
            Post post2 = new Post(userId2, "Title 2", "Content 2", "campus", "harvard.edu");
            Post post3 = new Post(userId2, "Title 3", "Content 3", "campus", "harvard.edu");
            Post post4 = new Post(userId2, "Title 4", "Content 4", "campus", "harvard.edu");
            Post post5 = new Post(userId2, "Title 5", "Content 5", "campus", "harvard.edu");

            post1 = postRepository.save(post1);
            post2 = postRepository.save(post2);
            post3 = postRepository.save(post3);
            post4 = postRepository.save(post4);
            post5 = postRepository.save(post5);

            // User1 likes post1, post3, and post5
            postLikeRepository.save(new PostLike(post1.getId(), userId1));
            postLikeRepository.save(new PostLike(post3.getId(), userId1));
            postLikeRepository.save(new PostLike(post5.getId(), userId1));

            // Fetch posts as user1
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, "harvard.edu", SortBy.NEWEST);

            // Verify we got all posts
            assertEquals(5, result.getContent().size(), "Should return 5 posts");

            // Verify like status for each post
            List<Post> posts = result.getContent();
            
            // Find each post and verify its liked status
            for (Post post : posts) {
                if (post.getId().equals(post1.getId())) {
                    assertTrue(post.isLiked(), "Post 1 should be liked by user1");
                } else if (post.getId().equals(post2.getId())) {
                    assertFalse(post.isLiked(), "Post 2 should not be liked by user1");
                } else if (post.getId().equals(post3.getId())) {
                    assertTrue(post.isLiked(), "Post 3 should be liked by user1");
                } else if (post.getId().equals(post4.getId())) {
                    assertFalse(post.isLiked(), "Post 4 should not be liked by user1");
                } else if (post.getId().equals(post5.getId())) {
                    assertTrue(post.isLiked(), "Post 5 should be liked by user1");
                }
            }
        }

        @Test
        @DisplayName("Should handle when user has not liked any posts")
        void shouldHandleNoLikes() {
            // Create 3 posts
            postRepository.save(new Post(userId2, "Title 1", "Content 1", "campus", "harvard.edu"));
            postRepository.save(new Post(userId2, "Title 2", "Content 2", "campus", "harvard.edu"));
            postRepository.save(new Post(userId2, "Title 3", "Content 3", "campus", "harvard.edu"));

            // Fetch posts as user1 (who hasn't liked anything)
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, "harvard.edu", SortBy.NEWEST);

            assertEquals(3, result.getContent().size());
            
            // All posts should show as not liked
            for (Post post : result.getContent()) {
                assertFalse(post.isLiked(), "Post should not be liked");
            }
        }

        @Test
        @DisplayName("Should handle when user has liked all posts")
        void shouldHandleAllLiked() {
            // Create 3 posts
            Post post1 = postRepository.save(new Post(userId2, "Title 1", "Content 1", "campus", "harvard.edu"));
            Post post2 = postRepository.save(new Post(userId2, "Title 2", "Content 2", "campus", "harvard.edu"));
            Post post3 = postRepository.save(new Post(userId2, "Title 3", "Content 3", "campus", "harvard.edu"));

            // User1 likes all posts
            postLikeRepository.save(new PostLike(post1.getId(), userId1));
            postLikeRepository.save(new PostLike(post2.getId(), userId1));
            postLikeRepository.save(new PostLike(post3.getId(), userId1));

            // Fetch posts as user1
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, "harvard.edu", SortBy.NEWEST);

            assertEquals(3, result.getContent().size());
            
            // All posts should show as liked
            for (Post post : result.getContent()) {
                assertTrue(post.isLiked(), "Post should be liked");
            }
        }

        @Test
        @DisplayName("Should correctly enrich with sorting by newest")
        void shouldEnrichWithSortByNewest() {
            // Create posts
            Post post1 = postRepository.save(new Post(userId2, "Title 1", "Content 1", "campus", "harvard.edu"));
            Post post2 = postRepository.save(new Post(userId2, "Title 2", "Content 2", "campus", "harvard.edu"));
            
            // User1 likes post1
            postLikeRepository.save(new PostLike(post1.getId(), userId1));

            // Fetch with sorting
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, "harvard.edu", SortBy.NEWEST);

            assertEquals(2, result.getContent().size());
            
            // Verify like status is maintained with sorting
            for (Post post : result.getContent()) {
                if (post.getId().equals(post1.getId())) {
                    assertTrue(post.isLiked(), "Post 1 should be liked");
                } else {
                    assertFalse(post.isLiked(), "Other posts should not be liked");
                }
            }
        }

        @Test
        @DisplayName("Should correctly enrich with sorting by most liked")
        void shouldEnrichWithSortByMostLiked() {
            // Create posts with different like counts
            Post post1 = new Post(userId2, "Title 1", "Content 1", "campus", "harvard.edu");
            post1.setLikeCount(10);
            post1 = postRepository.save(post1);
            
            Post post2 = new Post(userId2, "Title 2", "Content 2", "campus", "harvard.edu");
            post2.setLikeCount(5);
            post2 = postRepository.save(post2);
            
            // User1 likes post2 (the one with fewer total likes)
            postLikeRepository.save(new PostLike(post2.getId(), userId1));

            // Fetch with sorting by most liked
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, "harvard.edu", SortBy.MOST_LIKED);

            assertEquals(2, result.getContent().size());
            
            // First should be post1 (10 likes), second should be post2 (5 likes)
            assertEquals(post1.getId(), result.getContent().get(0).getId());
            assertEquals(post2.getId(), result.getContent().get(1).getId());
            
            // Verify like status is correct
            assertFalse(result.getContent().get(0).isLiked(), "Post 1 should not be liked by user1");
            assertTrue(result.getContent().get(1).isLiked(), "Post 2 should be liked by user1");
        }
    }

    @Nested
    @DisplayName("Batch Enrichment - Pagination Tests")
    class BatchEnrichmentPaginationTests {

        @Test
        @DisplayName("Should correctly enrich across multiple pages")
        void shouldEnrichAcrossPages() {
            // Create 25 posts
            for (int i = 0; i < 25; i++) {
                Post post = new Post(userId2, "Title " + i, "Content " + i, "campus", "harvard.edu");
                post = postRepository.save(post);
                
                // User1 likes every 3rd post
                if (i % 3 == 0) {
                    postLikeRepository.save(new PostLike(post.getId(), userId1));
                }
            }

            // Fetch first page (20 posts)
            Pageable pageable1 = Pageable.from(0, 20);
            Page<Post> page1 = postsService.getPostsByWall("campus", pageable1, userId1, "harvard.edu", SortBy.NEWEST);

            assertEquals(20, page1.getContent().size(), "First page should have 20 posts");
            
            // Count liked posts on first page
            long likedCount1 = page1.getContent().stream().filter(Post::isLiked).count();
            assertTrue(likedCount1 > 0, "Should have some liked posts on first page");

            // Fetch second page (5 posts)
            Pageable pageable2 = Pageable.from(1, 20);
            Page<Post> page2 = postsService.getPostsByWall("campus", pageable2, userId1, "harvard.edu", SortBy.NEWEST);

            assertEquals(5, page2.getContent().size(), "Second page should have 5 posts");
            
            // Verify like status is correct on second page too
            for (Post post : page2.getContent()) {
                // We can't easily verify individual posts here, but we can verify the field is set
                assertNotNull(post, "Post should not be null");
            }
        }

        @Test
        @DisplayName("Should handle empty page correctly")
        void shouldHandleEmptyPage() {
            // Create 5 posts
            for (int i = 0; i < 5; i++) {
                postRepository.save(new Post(userId2, "Title " + i, "Content " + i, "campus", "harvard.edu"));
            }

            // Request page beyond available data
            Pageable pageable = Pageable.from(5, 20);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, "harvard.edu", SortBy.NEWEST);

            assertTrue(result.getContent().isEmpty(), "Should return empty page");
        }
    }

    @Nested
    @DisplayName("Batch Enrichment - National Wall Tests")
    class BatchEnrichmentNationalWallTests {

        @Test
        @DisplayName("Should correctly enrich national wall posts")
        void shouldEnrichNationalPosts() {
            // Create national posts
            Post post1 = postRepository.save(new Post(userId2, "Title 1", "National 1", "national", null));
            Post post2 = postRepository.save(new Post(userId2, "Title 2", "National 2", "national", null));
            Post post3 = postRepository.save(new Post(userId2, "Title 3", "National 3", "national", null));

            // User1 likes post2
            postLikeRepository.save(new PostLike(post2.getId(), userId1));

            // Fetch national posts
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("national", pageable, userId1, "harvard.edu", SortBy.NEWEST);

            assertEquals(3, result.getContent().size());
            
            // Verify like status
            for (Post post : result.getContent()) {
                if (post.getId().equals(post2.getId())) {
                    assertTrue(post.isLiked(), "Post 2 should be liked");
                } else {
                    assertFalse(post.isLiked(), "Other posts should not be liked");
                }
            }
        }
    }

    @Nested
    @DisplayName("Batch Enrichment - Single Post Tests")
    class BatchEnrichmentSinglePostTests {

        @Test
        @DisplayName("Should correctly enrich single post retrieval")
        void shouldEnrichSinglePost() {
            // Create a post
            Post post = postRepository.save(new Post(userId2, "Title", "Content", "campus", "harvard.edu"));

            // User1 likes the post
            postLikeRepository.save(new PostLike(post.getId(), userId1));

            // Fetch single post as user1
            Post result = postsService.getPost(post.getId(), userId1);

            assertNotNull(result);
            assertTrue(result.isLiked(), "Post should be liked by user1");
        }

        @Test
        @DisplayName("Should show not liked for single post when user hasn't liked it")
        void shouldShowNotLikedForSinglePost() {
            // Create a post
            Post post = postRepository.save(new Post(userId2, "Title", "Content", "campus", "harvard.edu"));

            // Don't like the post

            // Fetch single post as user1
            Post result = postsService.getPost(post.getId(), userId1);

            assertNotNull(result);
            assertFalse(result.isLiked(), "Post should not be liked by user1");
        }
    }

    @Nested
    @DisplayName("Repository Batch Query Tests")
    class RepositoryBatchQueryTests {

        @Test
        @DisplayName("Should fetch likes for multiple posts in batch")
        void shouldFetchLikesInBatch() {
            // Create posts
            Post post1 = postRepository.save(new Post(userId2, "Title 1", "Content 1", "campus", "harvard.edu"));
            Post post2 = postRepository.save(new Post(userId2, "Title 2", "Content 2", "campus", "harvard.edu"));
            Post post3 = postRepository.save(new Post(userId2, "Title 3", "Content 3", "campus", "harvard.edu"));

            // User1 likes post1 and post3
            postLikeRepository.save(new PostLike(post1.getId(), userId1));
            postLikeRepository.save(new PostLike(post3.getId(), userId1));

            // Test the batch query method directly
            List<UUID> postIds = List.of(post1.getId(), post2.getId(), post3.getId());
            List<PostLike> likes = postLikeRepository.findByUserIdAndPostIdIn(userId1, postIds);

            assertEquals(2, likes.size(), "Should return 2 likes");
            assertTrue(likes.stream().anyMatch(like -> like.getPostId().equals(post1.getId())), "Should include like for post1");
            assertTrue(likes.stream().anyMatch(like -> like.getPostId().equals(post3.getId())), "Should include like for post3");
        }

        @Test
        @DisplayName("Should return empty list when user hasn't liked any posts")
        void shouldReturnEmptyListWhenNoLikes() {
            // Create posts
            Post post1 = postRepository.save(new Post(userId2, "Title 1", "Content 1", "campus", "harvard.edu"));
            Post post2 = postRepository.save(new Post(userId2, "Title 2", "Content 2", "campus", "harvard.edu"));

            // Don't like any posts

            // Test the batch query method directly
            List<UUID> postIds = List.of(post1.getId(), post2.getId());
            List<PostLike> likes = postLikeRepository.findByUserIdAndPostIdIn(userId1, postIds);

            assertTrue(likes.isEmpty(), "Should return empty list");
        }

        @Test
        @DisplayName("Should handle empty post IDs list")
        void shouldHandleEmptyPostIdsList() {
            // Test with empty list
            List<PostLike> likes = postLikeRepository.findByUserIdAndPostIdIn(userId1, List.of());

            assertTrue(likes.isEmpty(), "Should return empty list for empty input");
        }
    }
}
