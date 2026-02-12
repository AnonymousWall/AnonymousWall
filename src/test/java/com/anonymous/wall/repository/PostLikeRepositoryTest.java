package com.anonymous.wall.repository;

import com.anonymous.wall.entity.PostLike;
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

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
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
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            PostLike like = new PostLike(postId, userId);
            like.setCreatedAt(OffsetDateTime.now());

            // Act
            PostLike saved = repository.save(like);

            // Assert
            assertNotNull(saved);
            assertNotNull(saved.getId());
            assertEquals(postId, saved.getPostId());
            assertEquals(userId, saved.getUserId());
        }

        @Test
        @DisplayName("Positive: Should auto-generate UUID")
        void shouldAutoGenerateUuid() {
            // Arrange
            PostLike like = new PostLike(UUID.randomUUID(), UUID.randomUUID());
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
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            createTestLike(postId, userId);

            // Act
            Optional<PostLike> found = repository.findByPostIdAndUserId(postId, userId);

            // Assert
            assertTrue(found.isPresent());
            assertEquals(postId, found.get().getPostId());
            assertEquals(userId, found.get().getUserId());
        }

        @Test
        @DisplayName("Negative: Should return empty for non-existent like")
        void shouldReturnEmptyForNonExistentLike() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            // Act
            Optional<PostLike> found = repository.findByPostIdAndUserId(postId, userId);

            // Assert
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Edge: Should distinguish between different users")
        void shouldDistinguishBetweenDifferentUsers() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            createTestLike(postId, userId1);

            // Act
            Optional<PostLike> found1 = repository.findByPostIdAndUserId(postId, userId1);
            Optional<PostLike> found2 = repository.findByPostIdAndUserId(postId, userId2);

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
            UUID userId = UUID.randomUUID();
            UUID post1 = UUID.randomUUID();
            UUID post2 = UUID.randomUUID();
            UUID post3 = UUID.randomUUID();
            createTestLike(post1, userId);
            createTestLike(post2, userId);
            List<UUID> postIds = Arrays.asList(post1, post2, post3);

            // Act
            List<PostLike> likes = repository.findByUserIdAndPostIdIn(userId, postIds);

            // Assert
            assertNotNull(likes);
            assertEquals(2, likes.size());
        }

        @Test
        @DisplayName("Edge: Should return empty list when no likes exist")
        void shouldReturnEmptyListWhenNoLikesExist() {
            // Arrange
            UUID userId = UUID.randomUUID();
            List<UUID> postIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());

            // Act
            List<PostLike> likes = repository.findByUserIdAndPostIdIn(userId, postIds);

            // Assert
            assertNotNull(likes);
            assertTrue(likes.isEmpty());
        }

        @Test
        @DisplayName("Edge: Should handle empty post list")
        void shouldHandleEmptyPostList() {
            // Arrange
            UUID userId = UUID.randomUUID();
            List<UUID> emptyList = Arrays.asList();

            // Act
            List<PostLike> likes = repository.findByUserIdAndPostIdIn(userId, emptyList);

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
            UUID postId = UUID.randomUUID();
            createTestLike(postId, UUID.randomUUID());
            createTestLike(postId, UUID.randomUUID());
            createTestLike(postId, UUID.randomUUID());

            // Act
            long count = repository.countByPostId(postId);

            // Assert
            assertEquals(3, count);
        }

        @Test
        @DisplayName("Negative: Should return 0 for post with no likes")
        void shouldReturnZeroForPostWithNoLikes() {
            // Arrange
            UUID postId = UUID.randomUUID();

            // Act
            long count = repository.countByPostId(postId);

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
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            createTestLike(postId, userId);

            // Act
            long deleted = repository.deleteByPostIdAndUserId(postId, userId);

            // Assert
            assertEquals(1, deleted);
            Optional<PostLike> found = repository.findByPostIdAndUserId(postId, userId);
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Negative: Should return 0 when no like exists")
        void shouldReturnZeroWhenNoLikeExists() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            // Act
            long deleted = repository.deleteByPostIdAndUserId(postId, userId);

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
            UUID postId = UUID.randomUUID();
            createTestLike(postId, UUID.randomUUID());
            createTestLike(postId, UUID.randomUUID());
            createTestLike(postId, UUID.randomUUID());

            // Act
            long deleted = repository.deleteByPostId(postId);

            // Assert
            assertEquals(3, deleted);
            assertEquals(0, repository.countByPostId(postId));
        }

        @Test
        @DisplayName("Negative: Should return 0 for post with no likes")
        void shouldReturnZeroForPostWithNoLikes() {
            // Arrange
            UUID postId = UUID.randomUUID();

            // Act
            long deleted = repository.deleteByPostId(postId);

            // Assert
            assertEquals(0, deleted);
        }
    }
}
