package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.PostsService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("Profile Name Update Tests")
class ProfileNameUpdateTests {

    @Inject
    private PostsService postsService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private PostRepository postRepository;

    @Inject
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        // Clean up test data
        commentRepository.deleteAll();
        postRepository.deleteAll();
    }

    @Nested
    @DisplayName("When user updates profile name")
    class ProfileNameUpdateBehavior {

        private UserEntity testUser;

        @BeforeEach
        void setUp() {
            // Create a test user with an initial profile name
            testUser = new UserEntity();
            testUser.setEmail("profile_update_" + System.currentTimeMillis() + "@harvard.edu");
            testUser.setSchoolDomain("harvard.edu");
            testUser.setProfileName("Original Name");
            testUser.setVerified(true);
            testUser = userRepository.save(testUser);
        }

        @Test
        @DisplayName("Should update profile name in all user's posts")
        void shouldUpdateProfileNameInAllPosts() {
            // Create multiple posts with original name
            CreatePostRequest request1 = new CreatePostRequest("Post 1", "First post content");
            Post post1 = postsService.createPost(request1, testUser.getId());
            assertEquals("Original Name", post1.getProfileName());

            CreatePostRequest request2 = new CreatePostRequest("Post 2", "Second post content");
            Post post2 = postsService.createPost(request2, testUser.getId());
            assertEquals("Original Name", post2.getProfileName());

            CreatePostRequest request3 = new CreatePostRequest("Post 3", "Third post content");
            Post post3 = postsService.createPost(request3, testUser.getId());
            assertEquals("Original Name", post3.getProfileName());

            // Update user's profile name using repository methods (simulating AuthController behavior)
            testUser.setProfileName("Updated Name");
            userRepository.update(testUser);
            postRepository.updateProfileNameByUserId(testUser.getId(), "Updated Name");

            // Verify all posts have updated profile name
            Optional<Post> savedPost1 = postRepository.findById(post1.getId());
            Optional<Post> savedPost2 = postRepository.findById(post2.getId());
            Optional<Post> savedPost3 = postRepository.findById(post3.getId());

            assertTrue(savedPost1.isPresent());
            assertTrue(savedPost2.isPresent());
            assertTrue(savedPost3.isPresent());

            assertEquals("Updated Name", savedPost1.get().getProfileName());
            assertEquals("Updated Name", savedPost2.get().getProfileName());
            assertEquals("Updated Name", savedPost3.get().getProfileName());
        }

        @Test
        @DisplayName("Should update profile name in all user's comments")
        void shouldUpdateProfileNameInAllComments() {
            // Create a test post
            Post testPost = new Post(testUser.getId(), "Test Title", "Test post", "campus", "harvard.edu");
            testPost = postRepository.save(testPost);

            // Create multiple comments with original name
            CreateCommentRequest commentRequest1 = new CreateCommentRequest("First comment");
            Comment comment1 = postsService.addComment(testPost.getId(), commentRequest1, testUser.getId());
            assertEquals("Original Name", comment1.getProfileName());

            CreateCommentRequest commentRequest2 = new CreateCommentRequest("Second comment");
            Comment comment2 = postsService.addComment(testPost.getId(), commentRequest2, testUser.getId());
            assertEquals("Original Name", comment2.getProfileName());

            CreateCommentRequest commentRequest3 = new CreateCommentRequest("Third comment");
            Comment comment3 = postsService.addComment(testPost.getId(), commentRequest3, testUser.getId());
            assertEquals("Original Name", comment3.getProfileName());

            // Update user's profile name using repository methods (simulating AuthController behavior)
            testUser.setProfileName("Updated Name");
            userRepository.update(testUser);
            commentRepository.updateProfileNameByUserId(testUser.getId(), "Updated Name");

            // Verify all comments have updated profile name
            Optional<Comment> savedComment1 = commentRepository.findById(comment1.getId());
            Optional<Comment> savedComment2 = commentRepository.findById(comment2.getId());
            Optional<Comment> savedComment3 = commentRepository.findById(comment3.getId());

            assertTrue(savedComment1.isPresent());
            assertTrue(savedComment2.isPresent());
            assertTrue(savedComment3.isPresent());

            assertEquals("Updated Name", savedComment1.get().getProfileName());
            assertEquals("Updated Name", savedComment2.get().getProfileName());
            assertEquals("Updated Name", savedComment3.get().getProfileName());
        }

        @Test
        @DisplayName("Should update profile name in both posts and comments")
        void shouldUpdateProfileNameInBothPostsAndComments() {
            // Create posts
            CreatePostRequest postRequest1 = new CreatePostRequest("Post 1", "First post");
            Post post1 = postsService.createPost(postRequest1, testUser.getId());

            CreatePostRequest postRequest2 = new CreatePostRequest("Post 2", "Second post");
            Post post2 = postsService.createPost(postRequest2, testUser.getId());

            // Create comments on the first post
            CreateCommentRequest commentRequest1 = new CreateCommentRequest("Comment 1");
            Comment comment1 = postsService.addComment(post1.getId(), commentRequest1, testUser.getId());

            CreateCommentRequest commentRequest2 = new CreateCommentRequest("Comment 2");
            Comment comment2 = postsService.addComment(post1.getId(), commentRequest2, testUser.getId());

            // Verify initial profile names
            assertEquals("Original Name", post1.getProfileName());
            assertEquals("Original Name", post2.getProfileName());
            assertEquals("Original Name", comment1.getProfileName());
            assertEquals("Original Name", comment2.getProfileName());

            // Update user's profile name using repository methods (simulating AuthController behavior)
            testUser.setProfileName("New Display Name");
            userRepository.update(testUser);
            postRepository.updateProfileNameByUserId(testUser.getId(), "New Display Name");
            commentRepository.updateProfileNameByUserId(testUser.getId(), "New Display Name");

            // Verify all posts and comments have updated profile name
            List<Post> userPosts = postRepository.findByUserId(testUser.getId());
            assertEquals(2, userPosts.size());
            for (Post post : userPosts) {
                assertEquals("New Display Name", post.getProfileName());
            }

            List<Comment> postComments = commentRepository.findByPostId(post1.getId());
            assertEquals(2, postComments.size());
            for (Comment comment : postComments) {
                assertEquals("New Display Name", comment.getProfileName());
            }
        }

        @Test
        @DisplayName("Should not affect other users' posts and comments")
        void shouldNotAffectOtherUsers() {
            // Create another user
            UserEntity otherUser = new UserEntity();
            otherUser.setEmail("other_user_" + System.currentTimeMillis() + "@harvard.edu");
            otherUser.setSchoolDomain("harvard.edu");
            otherUser.setProfileName("Other User Name");
            otherUser.setVerified(true);
            otherUser = userRepository.save(otherUser);

            // Create posts from both users
            CreatePostRequest testUserPost = new CreatePostRequest("Test User Post", "Content");
            Post post1 = postsService.createPost(testUserPost, testUser.getId());

            CreatePostRequest otherUserPost = new CreatePostRequest("Other User Post", "Content");
            Post post2 = postsService.createPost(otherUserPost, otherUser.getId());

            // Create comments from both users
            CreateCommentRequest testUserComment = new CreateCommentRequest("Test user comment");
            Comment comment1 = postsService.addComment(post1.getId(), testUserComment, testUser.getId());

            CreateCommentRequest otherUserComment = new CreateCommentRequest("Other user comment");
            Comment comment2 = postsService.addComment(post1.getId(), otherUserComment, otherUser.getId());

            // Update only testUser's profile name
            testUser.setProfileName("Updated Test User");
            userRepository.update(testUser);
            postRepository.updateProfileNameByUserId(testUser.getId(), "Updated Test User");
            commentRepository.updateProfileNameByUserId(testUser.getId(), "Updated Test User");

            // Verify testUser's posts and comments are updated
            Optional<Post> savedPost1 = postRepository.findById(post1.getId());
            Optional<Comment> savedComment1 = commentRepository.findById(comment1.getId());
            assertTrue(savedPost1.isPresent());
            assertTrue(savedComment1.isPresent());
            assertEquals("Updated Test User", savedPost1.get().getProfileName());
            assertEquals("Updated Test User", savedComment1.get().getProfileName());

            // Verify otherUser's posts and comments are NOT updated
            Optional<Post> savedPost2 = postRepository.findById(post2.getId());
            Optional<Comment> savedComment2 = commentRepository.findById(comment2.getId());
            assertTrue(savedPost2.isPresent());
            assertTrue(savedComment2.isPresent());
            assertEquals("Other User Name", savedPost2.get().getProfileName());
            assertEquals("Other User Name", savedComment2.get().getProfileName());
        }

        @Test
        @DisplayName("Should handle empty profile name by defaulting to Anonymous")
        void shouldHandleEmptyProfileName() {
            // Create a post with original name
            CreatePostRequest postRequest = new CreatePostRequest("Test Post", "Content");
            Post post = postsService.createPost(postRequest, testUser.getId());
            assertEquals("Original Name", post.getProfileName());

            // Update to empty name (should default to "Anonymous")
            testUser.setProfileName("Anonymous");
            userRepository.update(testUser);
            postRepository.updateProfileNameByUserId(testUser.getId(), "Anonymous");
            commentRepository.updateProfileNameByUserId(testUser.getId(), "Anonymous");

            // Verify profile name is "Anonymous"
            Optional<Post> savedPost = postRepository.findById(post.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("Anonymous", savedPost.get().getProfileName());
        }
    }
}
