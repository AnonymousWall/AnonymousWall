package com.anonymous.wall.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("Profile Name Service Tests")
class ProfileNameServiceTests {

    @Inject
    private PostsService postsService;

    @Inject
    private CommentsService commentsService;

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
    @DisplayName("Post Creation with Profile Name")
    class PostCreationProfileNameTests {

        private UserEntity userDefaultName;
        private UserEntity userCustomName;

        @BeforeEach
        void setUp() {
            // User with default profile name
            userDefaultName = new UserEntity();
            userDefaultName.setEmail("post_default_" + System.currentTimeMillis() + "@harvard.edu");
            userDefaultName.setSchoolDomain("harvard.edu");
            userDefaultName.setProfileName("Anonymous");
            userDefaultName.setVerified(true);
            userDefaultName = userRepository.save(userDefaultName);

            // User with custom profile name
            userCustomName = new UserEntity();
            userCustomName.setEmail("post_custom_" + System.currentTimeMillis() + "@harvard.edu");
            userCustomName.setSchoolDomain("harvard.edu");
            userCustomName.setProfileName("Alice Wonder");
            userCustomName.setVerified(true);
            userCustomName = userRepository.save(userCustomName);
        }

        @Test
        @DisplayName("Post should capture user's current profile name at creation")
        void postShouldCaptureCurrentProfileName() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("Test Title", "Test post content");

            // Act
            Post post = postsService.createPost(request, userCustomName.getId());

            // Assert
            assertEquals("Alice Wonder", post.getProfileName());
            assertNotNull(post.getId());

            // Verify in database
            Optional<Post> savedPost = postRepository.findById(post.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("Alice Wonder", savedPost.get().getProfileName());
        }

        @Test
        @DisplayName("Post with default 'Anonymous' profile name")
        void postWithDefaultAnonymousProfileName() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("Test Title", "Anonymous post");

            // Act
            Post post = postsService.createPost(request, userDefaultName.getId());

            // Assert
            assertEquals("Anonymous", post.getProfileName());

            // Verify in database
            Optional<Post> savedPost = postRepository.findById(post.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("Anonymous", savedPost.get().getProfileName());
        }

        @Test
        @DisplayName("Multiple posts should capture profile name at each creation time")
        void multiplePostsCaptureProfileNameAtCreation() {
            // First post
            CreatePostRequest request1 = new CreatePostRequest("Test Title", "First post");

            Post post1 = postsService.createPost(request1, userDefaultName.getId());
            assertEquals("Anonymous", post1.getProfileName());

            // Change profile name
            userDefaultName.setProfileName("New Name");
            userRepository.update(userDefaultName);

            // Second post
            CreatePostRequest request2 = new CreatePostRequest("Test Title", "Second post");

            Post post2 = postsService.createPost(request2, userDefaultName.getId());
            assertEquals("New Name", post2.getProfileName());

            // Verify first post still has original name
            Optional<Post> savedPost1 = postRepository.findById(post1.getId());
            assertTrue(savedPost1.isPresent());
            assertEquals("Anonymous", savedPost1.get().getProfileName());
        }

        @Test
        @DisplayName("Post profile name should not be null")
        void postProfileNameShouldNotBeNull() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("Test Title", "Test content");

            // Act
            Post post = postsService.createPost(request, userCustomName.getId());

            // Assert
            assertNotNull(post.getProfileName());
            assertFalse(post.getProfileName().isEmpty());
        }
    }

    @Nested
    @DisplayName("Comment Creation with Profile Name")
    class CommentCreationProfileNameTests {

        private UserEntity userDefaultName;
        private UserEntity userCustomName;
        private Post testPost;

        @BeforeEach
        void setUp() {
            // User with default profile name
            userDefaultName = new UserEntity();
            userDefaultName.setEmail("comment_default_" + System.currentTimeMillis() + "@harvard.edu");
            userDefaultName.setSchoolDomain("harvard.edu");
            userDefaultName.setProfileName("Anonymous");
            userDefaultName.setVerified(true);
            userDefaultName = userRepository.save(userDefaultName);

            // User with custom profile name
            userCustomName = new UserEntity();
            userCustomName.setEmail("comment_custom_" + System.currentTimeMillis() + "@harvard.edu");
            userCustomName.setSchoolDomain("harvard.edu");
            userCustomName.setProfileName("Bob Builder");
            userCustomName.setVerified(true);
            userCustomName = userRepository.save(userCustomName);

            // Create test post
            testPost = new Post(userDefaultName.getId(), "Title", "Test post", "campus", "harvard.edu");
            testPost = postRepository.save(testPost);
        }

        @Test
        @DisplayName("Comment should capture user's current profile name at creation")
        void commentShouldCaptureCurrentProfileName() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("Test comment");

            // Act
            Comment comment = commentsService.addComment(testPost.getId(), request, userCustomName.getId());

            // Assert
            assertEquals("Bob Builder", comment.getProfileName());
            assertNotNull(comment.getId());

            // Verify in database
            Optional<Comment> savedComment = commentRepository.findById(comment.getId());
            assertTrue(savedComment.isPresent());
            assertEquals("Bob Builder", savedComment.get().getProfileName());
        }

        @Test
        @DisplayName("Comment with default 'Anonymous' profile name")
        void commentWithDefaultAnonymousProfileName() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("Anonymous comment");

            // Act
            Comment comment = commentsService.addComment(testPost.getId(), request, userDefaultName.getId());

            // Assert
            assertEquals("Anonymous", comment.getProfileName());

            // Verify in database
            Optional<Comment> savedComment = commentRepository.findById(comment.getId());
            assertTrue(savedComment.isPresent());
            assertEquals("Anonymous", savedComment.get().getProfileName());
        }

        @Test
        @DisplayName("Multiple comments should capture profile name at each creation time")
        void multipleCommentsCaptureProfileNameAtCreation() {
            // First comment
            CreateCommentRequest request1 = new CreateCommentRequest("First comment");

            Comment comment1 = commentsService.addComment(testPost.getId(), request1, userDefaultName.getId());
            assertEquals("Anonymous", comment1.getProfileName());

            // Change profile name
            userDefaultName.setProfileName("Changed Name");
            userRepository.update(userDefaultName);

            // Second comment
            CreateCommentRequest request2 = new CreateCommentRequest("Second comment");

            Comment comment2 = commentsService.addComment(testPost.getId(), request2, userDefaultName.getId());
            assertEquals("Changed Name", comment2.getProfileName());

            // Verify first comment still has original name
            Optional<Comment> savedComment1 = commentRepository.findById(comment1.getId());
            assertTrue(savedComment1.isPresent());
            assertEquals("Anonymous", savedComment1.get().getProfileName());
        }

        @Test
        @DisplayName("Comment profile name should not be null")
        void commentProfileNameShouldNotBeNull() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("Test comment");

            // Act
            Comment comment = commentsService.addComment(testPost.getId(), request, userCustomName.getId());

            // Assert
            assertNotNull(comment.getProfileName());
            assertFalse(comment.getProfileName().isEmpty());
        }

        @Test
        @DisplayName("Different users commenting should show different profile names")
        void differentUsersShowDifferentProfileNames() {
            // First comment from user with custom name
            CreateCommentRequest request1 = new CreateCommentRequest("Comment from Bob");

            Comment comment1 = commentsService.addComment(testPost.getId(), request1, userCustomName.getId());
            assertEquals("Bob Builder", comment1.getProfileName());

            // Second comment from user with default name
            CreateCommentRequest request2 = new CreateCommentRequest("Comment from Anonymous");

            Comment comment2 = commentsService.addComment(testPost.getId(), request2, userDefaultName.getId());
            assertEquals("Anonymous", comment2.getProfileName());

            // Verify both
            Optional<Comment> saved1 = commentRepository.findById(comment1.getId());
            Optional<Comment> saved2 = commentRepository.findById(comment2.getId());

            assertTrue(saved1.isPresent());
            assertTrue(saved2.isPresent());
            assertEquals("Bob Builder", saved1.get().getProfileName());
            assertEquals("Anonymous", saved2.get().getProfileName());
        }
    }

    @Nested
    @DisplayName("Profile Name Persistence Tests")
    class ProfileNamePersistenceTests {

        private UserEntity testUser;

        @BeforeEach
        void setUp() {
            testUser = new UserEntity();
            testUser.setEmail("persistence_" + System.currentTimeMillis() + "@harvard.edu");
            testUser.setSchoolDomain("harvard.edu");
            testUser.setProfileName("Original Name");
            testUser.setVerified(true);
            testUser = userRepository.save(testUser);
        }

        @Test
        @DisplayName("Post should retain profile name even if user changes their name")
        void postRetainsOriginalProfileName() {
            // Create post
            CreatePostRequest postRequest = new CreatePostRequest("Retain Title", "Post with original name");

            Post post = postsService.createPost(postRequest, testUser.getId());
            assertEquals("Original Name", post.getProfileName());

            // Change user's profile name
            testUser.setProfileName("Updated Name");
            userRepository.update(testUser);

            // Verify post still has original name
            Optional<Post> savedPost = postRepository.findById(post.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("Original Name", savedPost.get().getProfileName());

            // Verify user's name changed
            Optional<UserEntity> updatedUser = userRepository.findById(testUser.getId());
            assertTrue(updatedUser.isPresent());
            assertEquals("Updated Name", updatedUser.get().getProfileName());
        }

        @Test
        @DisplayName("Comment should retain profile name even if user changes their name")
        void commentRetainsOriginalProfileName() {
            // Create post
            Post testPost = new Post(testUser.getId(), "Title", "Test post", "campus", "harvard.edu");
            testPost = postRepository.save(testPost);

            // Create comment
            CreateCommentRequest commentRequest = new CreateCommentRequest("Comment with original name");

            Comment comment = commentsService.addComment(testPost.getId(), commentRequest, testUser.getId());
            assertEquals("Original Name", comment.getProfileName());

            // Change user's profile name
            testUser.setProfileName("Updated Name");
            userRepository.update(testUser);

            // Verify comment still has original name
            Optional<Comment> savedComment = commentRepository.findById(comment.getId());
            assertTrue(savedComment.isPresent());
            assertEquals("Original Name", savedComment.get().getProfileName());
        }
    }
}
