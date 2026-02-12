package com.anonymous.wall.repository;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.PostLike;
import com.anonymous.wall.entity.UserEntity;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PostLikeRepository Tests")
class PostLikeRepositoryTest {

    @Inject
    PostLikeRepository repository;

    @Inject
    PostRepository postRepository;

    @Inject
    UserRepository userRepository;

    private Post testPost;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Clean up in correct order (child records first)
        repository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new UserEntity();
        testUser.setEmail("testuser" + System.currentTimeMillis() + "@test.edu");
        testUser.setSchoolDomain("test.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);

        // Create test post
        testPost = new Post();
        testPost.setUserId(testUser.getId());
        testPost.setWall("national");
        testPost.setContent("Test post");
        testPost.setCreatedAt(OffsetDateTime.now());
        testPost = postRepository.save(testPost);
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    private PostLike createTestLike(UUID postId, UUID userId) {
        PostLike like = new PostLike(postId, userId);
        like.setCreatedAt(OffsetDateTime.now());
        return repository.save(like);
    }

    @Nested
    @DisplayName("Save Tests")
    class SaveTests {

        @Test
        @DisplayName("Positive: Should save new post like")
        void shouldSaveNewPostLike() {
            // Arrange
            PostLike like = new PostLike(testPost.getId(), testUser.getId());
            like.setCreatedAt(OffsetDateTime.now());

            // Act
            PostLike saved = repository.save(like);

            // Assert
            assertNotNull(saved);
            assertNotNull(saved.getId());
            assertEquals(testPost.getId(), saved.getPostId());
            assertEquals(testUser.getId(), saved.getUserId());
        }

        @Test
        @DisplayName("Positive: Should auto-generate UUID")
        void shouldAutoGenerateUuid() {
            // Arrange
            PostLike like = new PostLike(testPost.getId(), testUser.getId());
            like.setCreatedAt(OffsetDateTime.now());

            // Act
            PostLike saved = repository.save(like);

            // Assert
            assertNotNull(saved.getId());
        }
    }

    @Nested
    @DisplayName("FindByPostIdAndUserId Tests")
    class FindByPostIdAndUserIdTests {

        @Test
        @DisplayName("Positive: Should find like by post and user")
        void shouldFindLikeByPostAndUser() {
            // Arrange
            createTestLike(testPost.getId(), testUser.getId());

            // Act
            Optional<PostLike> found = repository.findByPostIdAndUserId(testPost.getId(), testUser.getId());

            // Assert
            assertTrue(found.isPresent());
            assertEquals(testPost.getId(), found.get().getPostId());
            assertEquals(testUser.getId(), found.get().getUserId());
        }

        @Test
        @DisplayName("Negative: Should return empty for non-existent like")
        void shouldReturnEmptyForNonExistentLike() {
            // Arrange - create another user
            UserEntity anotherUser = new UserEntity();
            anotherUser.setEmail("another" + System.currentTimeMillis() + "@test.edu");
            anotherUser.setSchoolDomain("test.edu");
            anotherUser.setVerified(true);
            anotherUser = userRepository.save(anotherUser);

            // Act
            Optional<PostLike> found = repository.findByPostIdAndUserId(testPost.getId(), anotherUser.getId());

            // Assert
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Edge: Should distinguish between different users")
        void shouldDistinguishBetweenDifferentUsers() {
            // Arrange
            UserEntity user2 = new UserEntity();
            user2.setEmail("user2" + System.currentTimeMillis() + "@test.edu");
            user2.setSchoolDomain("test.edu");
            user2.setVerified(true);
            user2 = userRepository.save(user2);
            
            createTestLike(testPost.getId(), testUser.getId());

            // Act
            Optional<PostLike> found1 = repository.findByPostIdAndUserId(testPost.getId(), testUser.getId());
            Optional<PostLike> found2 = repository.findByPostIdAndUserId(testPost.getId(), user2.getId());

            // Assert
            assertTrue(found1.isPresent());
            assertTrue(found2.isEmpty());
        }
    }

    @Nested
    @DisplayName("FindByUserIdAndPostIdIn Tests")
    class FindByUserIdAndPostIdInTests {

        @Test
        @DisplayName("Positive: Should find all likes by user for multiple posts")
        void shouldFindAllLikesByUserForMultiplePosts() {
            // Arrange
            Post post2 = new Post();
            post2.setUserId(testUser.getId());
            post2.setWall("national");
            post2.setContent("Test post 2");
            post2.setCreatedAt(OffsetDateTime.now());
            post2 = postRepository.save(post2);

            Post post3 = new Post();
            post3.setUserId(testUser.getId());
            post3.setWall("national");
            post3.setContent("Test post 3");
            post3.setCreatedAt(OffsetDateTime.now());
            post3 = postRepository.save(post3);

            createTestLike(testPost.getId(), testUser.getId());
            createTestLike(post2.getId(), testUser.getId());
            List<UUID> postIds = Arrays.asList(testPost.getId(), post2.getId(), post3.getId());

            // Act
            List<PostLike> likes = repository.findByUserIdAndPostIdIn(testUser.getId(), postIds);

            // Assert
            assertNotNull(likes);
            assertEquals(2, likes.size());
        }

        @Test
        @DisplayName("Edge: Should return empty list when no likes exist")
        void shouldReturnEmptyListWhenNoLikesExist() {
            // Arrange
            UserEntity anotherUser = new UserEntity();
            anotherUser.setEmail("nolikes" + System.currentTimeMillis() + "@test.edu");
            anotherUser.setSchoolDomain("test.edu");
            anotherUser.setVerified(true);
            anotherUser = userRepository.save(anotherUser);

            List<UUID> postIds = Arrays.asList(testPost.getId());

            // Act
            List<PostLike> likes = repository.findByUserIdAndPostIdIn(anotherUser.getId(), postIds);

            // Assert
            assertNotNull(likes);
            assertTrue(likes.isEmpty());
        }

        @Test
        @DisplayName("Edge: Should handle empty post list")
        void shouldHandleEmptyPostList() {
            // Arrange
            List<UUID> emptyList = Arrays.asList();

            // Act
            List<PostLike> likes = repository.findByUserIdAndPostIdIn(testUser.getId(), emptyList);

            // Assert
            assertNotNull(likes);
            assertTrue(likes.isEmpty());
        }
    }

    @Nested
    @DisplayName("CountByPostId Tests")
    class CountByPostIdTests {

        @Test
        @DisplayName("Positive: Should count likes for a post")
        void shouldCountLikesForPost() {
            // Arrange
            UserEntity user2 = new UserEntity();
            user2.setEmail("user2count" + System.currentTimeMillis() + "@test.edu");
            user2.setSchoolDomain("test.edu");
            user2.setVerified(true);
            user2 = userRepository.save(user2);

            UserEntity user3 = new UserEntity();
            user3.setEmail("user3count" + System.currentTimeMillis() + "@test.edu");
            user3.setSchoolDomain("test.edu");
            user3.setVerified(true);
            user3 = userRepository.save(user3);

            createTestLike(testPost.getId(), testUser.getId());
            createTestLike(testPost.getId(), user2.getId());
            createTestLike(testPost.getId(), user3.getId());

            // Act
            long count = repository.countByPostId(testPost.getId());

            // Assert
            assertEquals(3, count);
        }

        @Test
        @DisplayName("Negative: Should return 0 for post with no likes")
        void shouldReturnZeroForPostWithNoLikes() {
            // Arrange
            Post postWithNoLikes = new Post();
            postWithNoLikes.setUserId(testUser.getId());
            postWithNoLikes.setWall("national");
            postWithNoLikes.setContent("No likes post");
            postWithNoLikes.setCreatedAt(OffsetDateTime.now());
            postWithNoLikes = postRepository.save(postWithNoLikes);

            // Act
            long count = repository.countByPostId(postWithNoLikes.getId());

            // Assert
            assertEquals(0, count);
        }
    }

    @Nested
    @DisplayName("DeleteByPostIdAndUserId Tests")
    class DeleteByPostIdAndUserIdTests {

        @Test
        @DisplayName("Positive: Should delete like by post and user")
        void shouldDeleteLikeByPostAndUser() {
            // Arrange
            createTestLike(testPost.getId(), testUser.getId());

            // Act
            long deleted = repository.deleteByPostIdAndUserId(testPost.getId(), testUser.getId());

            // Assert
            assertEquals(1, deleted);
            Optional<PostLike> found = repository.findByPostIdAndUserId(testPost.getId(), testUser.getId());
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Negative: Should return 0 when no like exists")
        void shouldReturnZeroWhenNoLikeExists() {
            // Arrange
            UserEntity anotherUser = new UserEntity();
            anotherUser.setEmail("delete" + System.currentTimeMillis() + "@test.edu");
            anotherUser.setSchoolDomain("test.edu");
            anotherUser.setVerified(true);
            anotherUser = userRepository.save(anotherUser);

            // Act
            long deleted = repository.deleteByPostIdAndUserId(testPost.getId(), anotherUser.getId());

            // Assert
            assertEquals(0, deleted);
        }
    }

    @Nested
    @DisplayName("DeleteByPostId Tests")
    class DeleteByPostIdTests {

        @Test
        @DisplayName("Positive: Should delete all likes for a post")
        void shouldDeleteAllLikesForPost() {
            // Arrange
            UserEntity user2 = new UserEntity();
            user2.setEmail("user2del" + System.currentTimeMillis() + "@test.edu");
            user2.setSchoolDomain("test.edu");
            user2.setVerified(true);
            user2 = userRepository.save(user2);

            UserEntity user3 = new UserEntity();
            user3.setEmail("user3del" + System.currentTimeMillis() + "@test.edu");
            user3.setSchoolDomain("test.edu");
            user3.setVerified(true);
            user3 = userRepository.save(user3);

            createTestLike(testPost.getId(), testUser.getId());
            createTestLike(testPost.getId(), user2.getId());
            createTestLike(testPost.getId(), user3.getId());

            // Act
            long deleted = repository.deleteByPostId(testPost.getId());

            // Assert
            assertEquals(3, deleted);
            assertEquals(0, repository.countByPostId(testPost.getId()));
        }

        @Test
        @DisplayName("Negative: Should return 0 for post with no likes")
        void shouldReturnZeroForPostWithNoLikes() {
            // Arrange
            Post postNoLikes = new Post();
            postNoLikes.setUserId(testUser.getId());
            postNoLikes.setWall("national");
            postNoLikes.setContent("Post with no likes");
            postNoLikes.setCreatedAt(OffsetDateTime.now());
            postNoLikes = postRepository.save(postNoLikes);

            // Act
            long deleted = repository.deleteByPostId(postNoLikes.getId());

            // Assert
            assertEquals(0, deleted);
        }
    }
}
