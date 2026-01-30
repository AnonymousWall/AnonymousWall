package com.anonymous.wall.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Posts Service - Hide/Unhide Post Tests")
class PostsServiceHidePostTests {

    @Inject
    PostsService postsService;

    @Inject
    PostRepository postRepository;

    @Inject
    CommentRepository commentRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    PostLikeRepository postLikeRepository;

    private UserEntity testUser;
    private UserEntity otherUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // Clean up
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new UserEntity();
        testUser.setEmail("test" + System.currentTimeMillis() + "@harvard.edu");
        testUser.setSchoolDomain("harvard.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);

        // Create other user
        otherUser = new UserEntity();
        otherUser.setEmail("other" + System.currentTimeMillis() + "@harvard.edu");
        otherUser.setSchoolDomain("harvard.edu");
        otherUser.setVerified(true);
        otherUser.setPasswordSet(true);
        otherUser = userRepository.save(otherUser);

        // Create test post
        testPost = new Post(testUser.getId(), "Test post", "campus", "harvard.edu");
        testPost = postRepository.save(testPost);
    }

    @AfterEach
    void tearDown() {
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== HIDE POST TESTS ====================

    @Nested
    @DisplayName("Hide Post - Basic Functionality")
    class HidePostBasicTests {

        @Test
        @DisplayName("Should hide post successfully")
        void shouldHidePostSuccessfully() {
            // Act
            Post hiddenPost = postsService.hidePost(testPost.getId(), testUser.getId());

            // Assert
            assertTrue(hiddenPost.isHidden());
            assertEquals(testPost.getId(), hiddenPost.getId());

            // Verify in database
            Post dbPost = postRepository.findById(testPost.getId()).get();
            assertTrue(dbPost.isHidden());
        }

        @Test
        @DisplayName("Should throw exception if post not found")
        void shouldThrowExceptionForNonExistentPost() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    postsService.hidePost(99999L, testUser.getId())
            );
            assertEquals("Post not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception if user is not post author")
        void shouldThrowExceptionIfNotAuthor() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    postsService.hidePost(testPost.getId(), otherUser.getId())
            );
            assertEquals("You can only hide your own posts", exception.getMessage());
        }

        @Test
        @DisplayName("Should be idempotent when hiding already hidden post")
        void shouldBeIdempotentWhenHiding() {
            // First hide
            postsService.hidePost(testPost.getId(), testUser.getId());

            // Second hide should not throw exception
            Post hiddenAgain = postsService.hidePost(testPost.getId(), testUser.getId());

            // Assert
            assertTrue(hiddenAgain.isHidden());
        }
    }

    @Nested
    @DisplayName("Hide Post - Comment Cascade")
    class HidePostCommentCascadeTests {

        @Test
        @DisplayName("Should hide all comments when hiding post")
        void shouldHideAllCommentsWhenHidingPost() {
            // Arrange - Create 3 comments
            Comment comment1 = postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("Comment 1"), testUser.getId());
            Comment comment2 = postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("Comment 2"), otherUser.getId());
            Comment comment3 = postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("Comment 3"), testUser.getId());

            // Verify comments are visible
            List<Comment> visibleBefore = commentRepository.findByPostIdAndHiddenFalse(testPost.getId());
            assertEquals(3, visibleBefore.size());

            // Act - Hide post
            postsService.hidePost(testPost.getId(), testUser.getId());

            // Assert - All comments should be hidden
            List<Comment> visibleAfter = commentRepository.findByPostIdAndHiddenFalse(testPost.getId());
            assertEquals(0, visibleAfter.size());

            // Verify each comment is marked as hidden
            assertTrue(commentRepository.findById(comment1.getId()).get().isHidden());
            assertTrue(commentRepository.findById(comment2.getId()).get().isHidden());
            assertTrue(commentRepository.findById(comment3.getId()).get().isHidden());
        }

        @Test
        @DisplayName("Should handle post with no comments")
        void shouldHandlePostWithoutComments() {
            // Act - Hide post with no comments
            Post hiddenPost = postsService.hidePost(testPost.getId(), testUser.getId());

            // Assert
            assertTrue(hiddenPost.isHidden());
            assertEquals(0, hiddenPost.getCommentCount());
        }

        @Test
        @DisplayName("Should hide comments from multiple users")
        void shouldHideCommentsFromMultipleUsers() {
            // Arrange - Create comments from both users
            postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("User1 comment 1"), testUser.getId());
            postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("User2 comment 1"), otherUser.getId());
            postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("User1 comment 2"), testUser.getId());
            postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("User2 comment 2"), otherUser.getId());

            // Act - Hide post
            postsService.hidePost(testPost.getId(), testUser.getId());

            // Assert - All comments hidden regardless of author
            List<Comment> visibleComments = commentRepository.findByPostIdAndHiddenFalse(testPost.getId());
            assertEquals(0, visibleComments.size());

            // All comments should be in hidden state
            List<Comment> allComments = commentRepository.findAllByPostId(testPost.getId());
            for (Comment comment : allComments) {
                assertTrue(comment.isHidden());
            }
        }
    }

    // ==================== UNHIDE POST TESTS ====================

    @Nested
    @DisplayName("Unhide Post - Basic Functionality")
    class UnhidePostBasicTests {

        @Test
        @DisplayName("Should unhide post successfully")
        void shouldUnhidePostSuccessfully() {
            // Arrange - Hide post first
            postsService.hidePost(testPost.getId(), testUser.getId());

            // Act
            Post unHiddenPost = postsService.unhidePost(testPost.getId(), testUser.getId());

            // Assert
            assertFalse(unHiddenPost.isHidden());
            assertEquals(testPost.getId(), unHiddenPost.getId());

            // Verify in database
            Post dbPost = postRepository.findById(testPost.getId()).get();
            assertFalse(dbPost.isHidden());
        }

        @Test
        @DisplayName("Should throw exception if post not found")
        void shouldThrowExceptionForNonExistentPost() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    postsService.unhidePost(99999L, testUser.getId())
            );
            assertEquals("Post not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception if user is not post author")
        void shouldThrowExceptionIfNotAuthor() {
            // Arrange - Hide post as author
            postsService.hidePost(testPost.getId(), testUser.getId());

            // Act & Assert - Try to unhide as other user
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    postsService.unhidePost(testPost.getId(), otherUser.getId())
            );
            assertEquals("You can only unhide your own posts", exception.getMessage());
        }

        @Test
        @DisplayName("Should be idempotent when unhiding already visible post")
        void shouldBeIdempotentWhenUnhiding() {
            // Unhide a post that is not hidden
            Post unHiddenPost = postsService.unhidePost(testPost.getId(), testUser.getId());

            // Assert
            assertFalse(unHiddenPost.isHidden());
        }
    }

    @Nested
    @DisplayName("Unhide Post - Comment Restoration")
    class UnhidePostCommentRestorationTests {

        @Test
        @DisplayName("Should restore all comments when unhiding post")
        void shouldRestoreAllCommentsWhenUnhidingPost() {
            // Arrange - Create comments and hide post
            Comment comment1 = postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("Comment 1"), testUser.getId());
            Comment comment2 = postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("Comment 2"), otherUser.getId());

            // Hide post (cascades to comments)
            postsService.hidePost(testPost.getId(), testUser.getId());

            // Verify comments are hidden
            List<Comment> hiddenComments = commentRepository.findByPostIdAndHiddenFalse(testPost.getId());
            assertEquals(0, hiddenComments.size());

            // Act - Unhide post
            postsService.unhidePost(testPost.getId(), testUser.getId());

            // Assert - Comments should be restored
            List<Comment> restoredComments = commentRepository.findByPostIdAndHiddenFalse(testPost.getId());
            assertEquals(2, restoredComments.size());

            // Verify each comment is unhidden
            assertFalse(commentRepository.findById(comment1.getId()).get().isHidden());
            assertFalse(commentRepository.findById(comment2.getId()).get().isHidden());
        }

        @Test
        @DisplayName("Should handle unhiding post with no comments")
        void shouldHandlePostWithoutComments() {
            // Arrange - Hide post
            postsService.hidePost(testPost.getId(), testUser.getId());

            // Act - Unhide post
            Post unHiddenPost = postsService.unhidePost(testPost.getId(), testUser.getId());

            // Assert
            assertFalse(unHiddenPost.isHidden());
            assertEquals(0, unHiddenPost.getCommentCount());
        }

        @Test
        @DisplayName("Should preserve comment count through hide/unhide cycle")
        void shouldPreserveCommentCount() {
            // Arrange - Create comments
            postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("Comment 1"), testUser.getId());
            postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("Comment 2"), otherUser.getId());
            postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("Comment 3"), testUser.getId());

            Post initialPost = postRepository.findById(testPost.getId()).get();
            int initialCount = initialPost.getCommentCount();

            // Act - Hide and unhide
            postsService.hidePost(testPost.getId(), testUser.getId());
            postsService.unhidePost(testPost.getId(), testUser.getId());

            // Assert
            Post finalPost = postRepository.findById(testPost.getId()).get();
            assertEquals(initialCount, finalPost.getCommentCount());
            assertEquals(3, finalPost.getCommentCount());
        }
    }

    // ==================== QUERY FILTERING TESTS ====================

    @Nested
    @DisplayName("Query Filtering - Hidden Posts")
    class QueryFilteringTests {

        @Test
        @DisplayName("Hidden posts should not appear in getPostsByWall")
        void shouldFilterHiddenPostsFromWallQuery() {
            // Arrange - Create another post and hide the first one
            Post post2 = new Post(otherUser.getId(), "Other post", "campus", "harvard.edu");
            post2 = postRepository.save(post2);

            // Hide first post
            postsService.hidePost(testPost.getId(), testUser.getId());

            // Act & Assert
            io.micronaut.data.model.Page<Post> posts =
                    postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc(
                            "campus", "harvard.edu",
                            io.micronaut.data.model.Pageable.from(0, 20)
                    );

            // Only post2 should be visible
            assertEquals(1, posts.getContent().size());
            assertEquals(post2.getId(), posts.getContent().get(0).getId());
        }

        @Test
        @DisplayName("Unhidden posts should reappear in queries")
        void shouldIncludeUnHiddenPostsInQueries() {
            // Arrange - Hide post and create another
            Post post2 = new Post(otherUser.getId(), "Other post", "campus", "harvard.edu");
            post2 = postRepository.save(post2);
            postsService.hidePost(testPost.getId(), testUser.getId());

            // Act - Unhide post
            postsService.unhidePost(testPost.getId(), testUser.getId());

            // Assert - Both posts should be visible
            io.micronaut.data.model.Page<Post> posts =
                    postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc(
                            "campus", "harvard.edu",
                            io.micronaut.data.model.Pageable.from(0, 20)
                    );

            assertEquals(2, posts.getContent().size());
        }
    }

    // ==================== EDGE CASE TESTS ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle hiding post with liked comments")
        void shouldHidePostWithLikedComments() {
            // Arrange - Create comment and like (if applicable)
            Comment comment = postsService.addComment(testPost.getId(),
                    new CreateCommentRequest("Test comment"), testUser.getId());

            // Act - Hide post
            Post hiddenPost = postsService.hidePost(testPost.getId(), testUser.getId());

            // Assert
            assertTrue(hiddenPost.isHidden());
            assertTrue(commentRepository.findById(comment.getId()).get().isHidden());
        }

        @Test
        @DisplayName("Should handle national posts")
        void shouldHideNationalPost() {
            // Arrange - Create national post
            Post nationalPost = new Post(testUser.getId(), "National post", "national", null);
            nationalPost = postRepository.save(nationalPost);

            // Add comment
            Comment comment = postsService.addComment(nationalPost.getId(),
                    new CreateCommentRequest("National comment"), testUser.getId());

            // Act - Hide post
            Post hiddenPost = postsService.hidePost(nationalPost.getId(), testUser.getId());

            // Assert
            assertTrue(hiddenPost.isHidden());
            assertTrue(commentRepository.findById(comment.getId()).get().isHidden());
        }

        @Test
        @DisplayName("Should maintain transaction integrity on hide")
        void shouldMaintainTransactionIntegrity() {
            // Arrange - Create multiple comments
            for (int i = 0; i < 10; i++) {
                postsService.addComment(testPost.getId(),
                        new CreateCommentRequest("Comment " + i),
                        (i % 2 == 0) ? testUser.getId() : otherUser.getId());
            }

            // Act - Hide post
            Post hiddenPost = postsService.hidePost(testPost.getId(), testUser.getId());

            // Assert - All should be hidden atomically
            assertTrue(hiddenPost.isHidden());
            List<Comment> visibleComments = commentRepository.findByPostIdAndHiddenFalse(testPost.getId());
            assertEquals(0, visibleComments.size());

            List<Comment> allComments = commentRepository.findAllByPostId(testPost.getId());
            assertEquals(10, allComments.size());
            for (Comment comment : allComments) {
                assertTrue(comment.isHidden());
            }
        }
    }
}
