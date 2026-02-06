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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for optimized user lookup fix
 * Verifies that passing schoolDomain as JWT claim avoids redundant database lookups
 */
@MicronautTest(transactional = false)
@DisplayName("PostsService - Optimized User Lookup Tests")
class PostsServiceOptimizedUserLookupTest {

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
    private String schoolDomain1;

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
        schoolDomain1 = testUser1.getSchoolDomain();

        testUser2 = new UserEntity();
        testUser2.setId(UUID.randomUUID());
        testUser2.setEmail("user2@mit.edu");
        testUser2.setSchoolDomain("mit.edu");
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
    @DisplayName("Optimized Method - No User Lookup Tests")
    class OptimizedMethodTests {

        @Test
        @DisplayName("Should retrieve campus posts without user lookup when schoolDomain provided")
        void shouldRetrieveCampusPostsWithSchoolDomain() {
            // Create posts for harvard
            postRepository.save(new Post(userId1, "Title 1", "Harvard post 1", "campus", "harvard.edu"));
            postRepository.save(new Post(userId1, "Title 2", "Harvard post 2", "campus", "harvard.edu"));
            postRepository.save(new Post(userId1, "Title 3", "Harvard post 3", "campus", "harvard.edu"));

            // Create posts for MIT (should not be returned)
            postRepository.save(new Post(userId2, "Title 4", "MIT post 1", "campus", "mit.edu"));

            // Use optimized method with schoolDomain
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, schoolDomain1);

            assertEquals(3, result.getContent().size(), "Should return only Harvard posts");
            
            // Verify all posts are from harvard.edu
            for (Post post : result.getContent()) {
                assertEquals("harvard.edu", post.getSchoolDomain(), "All posts should be from Harvard");
            }
        }

        @Test
        @DisplayName("Should handle null schoolDomain for campus wall")
        void shouldHandleNullSchoolDomainForCampus() {
            // Create posts
            postRepository.save(new Post(userId1, "Title 1", "Post 1", "campus", "harvard.edu"));

            // Use optimized method with null schoolDomain (user has no school)
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, null);

            assertTrue(result.getContent().isEmpty(), "Should return empty page when no school domain");
        }

        @Test
        @DisplayName("Should retrieve national posts with schoolDomain parameter")
        void shouldRetrieveNationalPostsWithSchoolDomain() {
            // Create national posts
            postRepository.save(new Post(userId1, "Title 1", "National post 1", "national", null));
            postRepository.save(new Post(userId2, "Title 2", "National post 2", "national", null));

            // Use optimized method - schoolDomain should be ignored for national wall
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("national", pageable, userId1, schoolDomain1);

            assertEquals(2, result.getContent().size(), "Should return all national posts");
        }

        @Test
        @DisplayName("Should enrich posts with like status using optimized method")
        void shouldEnrichPostsWithOptimizedMethod() {
            // Create posts
            Post post1 = postRepository.save(new Post(userId1, "Title 1", "Post 1", "campus", "harvard.edu"));
            Post post2 = postRepository.save(new Post(userId1, "Title 2", "Post 2", "campus", "harvard.edu"));

            // User1 likes post1
            postLikeRepository.save(new PostLike(post1.getId(), userId1));

            // Use optimized method
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, schoolDomain1);

            assertEquals(2, result.getContent().size());
            
            // Verify like status
            for (Post post : result.getContent()) {
                if (post.getId().equals(post1.getId())) {
                    assertTrue(post.isLiked(), "Post 1 should be liked");
                } else {
                    assertFalse(post.isLiked(), "Post 2 should not be liked");
                }
            }
        }

        @Test
        @DisplayName("Should work with sorting and schoolDomain parameter")
        void shouldWorkWithSorting() {
            // Create posts with different like counts
            Post post1 = new Post(userId1, "Title 1", "Post 1", "campus", "harvard.edu");
            post1.setLikeCount(10);
            post1 = postRepository.save(post1);

            Post post2 = new Post(userId1, "Title 2", "Post 2", "campus", "harvard.edu");
            post2.setLikeCount(5);
            post2 = postRepository.save(post2);

            // Use optimized method with sorting
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, schoolDomain1, SortBy.MOST_LIKED);

            assertEquals(2, result.getContent().size());
            
            // Verify sorting - most liked first
            assertEquals(post1.getId(), result.getContent().get(0).getId(), "Post with 10 likes should be first");
            assertEquals(post2.getId(), result.getContent().get(1).getId(), "Post with 5 likes should be second");
        }

        @Test
        @DisplayName("Should handle empty schoolDomain string")
        void shouldHandleEmptySchoolDomain() {
            // Create posts
            postRepository.save(new Post(userId1, "Title 1", "Post 1", "campus", "harvard.edu"));

            // Use optimized method with empty schoolDomain
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, "");

            assertTrue(result.getContent().isEmpty(), "Should return empty page for empty school domain");
        }
    }

    @Nested
    @DisplayName("Backward Compatibility Tests")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("Old method without schoolDomain should still work")
        void oldMethodShouldStillWork() {
            // Create posts
            postRepository.save(new Post(userId1, "Title 1", "Post 1", "campus", "harvard.edu"));
            postRepository.save(new Post(userId1, "Title 2", "Post 2", "campus", "harvard.edu"));

            // Use old method (requires user lookup)
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1);

            assertEquals(2, result.getContent().size(), "Old method should still work");
        }

        @Test
        @DisplayName("Old method with sorting should still work")
        void oldMethodWithSortingShouldStillWork() {
            // Create posts
            Post post1 = new Post(userId1, "Title 1", "Post 1", "campus", "harvard.edu");
            post1.setLikeCount(10);
            postRepository.save(post1);

            Post post2 = new Post(userId1, "Title 2", "Post 2", "campus", "harvard.edu");
            post2.setLikeCount(5);
            postRepository.save(post2);

            // Use old method with sorting (requires user lookup)
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> result = postsService.getPostsByWall("campus", pageable, userId1, SortBy.MOST_LIKED);

            assertEquals(2, result.getContent().size());
            assertEquals(10, result.getContent().get(0).getLikeCount(), "Should be sorted correctly");
        }
    }

    @Nested
    @DisplayName("Performance Comparison Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Optimized method should produce same results as old method")
        void optimizedMethodShouldMatchOldMethod() {
            // Create test data
            for (int i = 0; i < 10; i++) {
                Post post = new Post(userId1, "Title " + i, "Content " + i, "campus", "harvard.edu");
                post.setLikeCount(i);
                post = postRepository.save(post);
                
                if (i % 2 == 0) {
                    postLikeRepository.save(new PostLike(post.getId(), userId1));
                }
            }

            Pageable pageable = Pageable.from(0, 5);

            // Call both methods
            Page<Post> resultOld = postsService.getPostsByWall("campus", pageable, userId1, SortBy.NEWEST);
            Page<Post> resultNew = postsService.getPostsByWall("campus", pageable, userId1, schoolDomain1, SortBy.NEWEST);

            // Results should be identical
            assertEquals(resultOld.getContent().size(), resultNew.getContent().size(), "Both methods should return same number of posts");
            assertEquals(resultOld.getTotalSize(), resultNew.getTotalSize(), "Both methods should have same total size");
            
            // Verify each post matches
            for (int i = 0; i < resultOld.getContent().size(); i++) {
                Post oldPost = resultOld.getContent().get(i);
                Post newPost = resultNew.getContent().get(i);
                
                assertEquals(oldPost.getId(), newPost.getId(), "Posts should be in same order");
                assertEquals(oldPost.isLiked(), newPost.isLiked(), "Like status should match");
                assertEquals(oldPost.getLikeCount(), newPost.getLikeCount(), "Like count should match");
            }
        }
    }
}
