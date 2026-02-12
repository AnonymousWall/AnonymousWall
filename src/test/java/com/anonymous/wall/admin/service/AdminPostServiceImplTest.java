package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.PostRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AdminPostServiceImpl Tests")
class AdminPostServiceImplTest {

    private AdminPostServiceImpl service;
    private PostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        service = new AdminPostServiceImpl();
        
        // Inject mock via reflection
        try {
            var repoField = AdminPostServiceImpl.class.getDeclaredField("postRepository");
            repoField.setAccessible(true);
            repoField.set(service, postRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject repository", e);
        }
    }

    private Post createMockPost(UUID id, String wall, String schoolDomain, UUID userId, boolean hidden) {
        Post post = new Post();
        post.setId(id);
        post.setWall(wall);
        post.setSchoolDomain(schoolDomain);
        post.setUserId(userId);
        post.setHidden(hidden);
        post.setCreatedAt(OffsetDateTime.now());
        post.setLikeCount(0);
        post.setCommentCount(0);
        return post;
    }

    @Nested
    @DisplayName("GetAllPosts Tests - No Filters")
    class GetAllPostsNoFiltersTests {

        @Test
        @DisplayName("Positive: Should return all posts with default pagination")
        void shouldReturnAllPostsWithDefaultPagination() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, null, null, null, null);

            // Assert
            assertNotNull(result);
            verify(postRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by createdAt descending")
        void shouldSortByCreatedAtDescending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, null, null, "createdAt", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by createdAt ascending")
        void shouldSortByCreatedAtAscending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, null, null, "createdAt", "asc");

            // Assert
            assertNotNull(result);
            verify(postRepository).findAllOrderByCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by likeCount descending")
        void shouldSortByLikeCountDescending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByLikeCountDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, null, null, "likeCount", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository).findAllOrderByLikeCountDesc(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by commentCount descending")
        void shouldSortByCommentCountDescending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCommentCountDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, null, null, "commentCount", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository).findAllOrderByCommentCountDesc(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by userId descending")
        void shouldSortByUserIdDescending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByUserIdDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, null, null, "userId", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository).findAllOrderByUserIdDesc(pageable);
        }

        @Test
        @DisplayName("Edge: Should handle 'author' as alias for userId")
        void shouldHandleAuthorAsAlias() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByUserIdDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, null, null, "author", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository).findAllOrderByUserIdDesc(pageable);
        }

        @Test
        @DisplayName("Edge: Should be case-insensitive for sortBy")
        void shouldBeCaseInsensitiveForSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByLikeCountDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, null, null, "LIKECOUNT", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository).findAllOrderByLikeCountDesc(pageable);
        }

        @Test
        @DisplayName("Negative: Should fallback to findAll for invalid sortBy")
        void shouldFallbackToFindAllForInvalidSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, null, null, "invalid", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("GetAllPosts Tests - With Filters")
    class GetAllPostsWithFiltersTests {

        @Test
        @DisplayName("Positive: Should filter by hidden status only")
        void shouldFilterByHiddenStatusOnly() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByHidden(true, pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, null, true, null, null);

            // Assert
            assertNotNull(result);
            verify(postRepository).findByHidden(true, pageable);
        }

        @Test
        @DisplayName("Positive: Should filter by userId only")
        void shouldFilterByUserIdOnly() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByUserId(userId, pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, userId, null, null, null);

            // Assert
            assertNotNull(result);
            verify(postRepository).findByUserId(userId, pageable);
        }

        @Test
        @DisplayName("Positive: Should filter by both userId and hidden")
        void shouldFilterByBothUserIdAndHidden() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByUserIdAndHidden(userId, false, pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getAllPosts(pageable, userId, false, null, null);

            // Assert
            assertNotNull(result);
            verify(postRepository).findByUserIdAndHidden(userId, false, pageable);
        }
    }

    @Nested
    @DisplayName("DeletePost Tests")
    class DeletePostTests {

        @Test
        @DisplayName("Positive: Should soft-delete post by setting hidden to true")
        void shouldSoftDeletePost() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.update(any(Post.class))).thenReturn(post);

            // Act
            service.deletePost(postId);

            // Assert
            assertTrue(post.isHidden());
            verify(postRepository).findById(postId);
            verify(postRepository).update(argThat(p -> p.isHidden()));
        }

        @Test
        @DisplayName("Negative: Should throw exception for non-existent post")
        void shouldThrowExceptionForNonExistentPost() {
            // Arrange
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.deletePost(postId)
            );
            assertTrue(exception.getMessage().contains("Post not found"));
            verify(postRepository).findById(postId);
            verify(postRepository, never()).update(any());
        }

        @Test
        @DisplayName("Edge: Should handle already hidden post")
        void shouldHandleAlreadyHiddenPost() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), true);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.update(any(Post.class))).thenReturn(post);

            // Act
            service.deletePost(postId);

            // Assert
            assertTrue(post.isHidden());
            verify(postRepository).update(post);
        }
    }

    @Nested
    @DisplayName("GetPostsByWall Tests")
    class GetPostsByWallTests {

        @Test
        @DisplayName("Positive: Should get national posts sorted by NEWEST")
        void shouldGetNationalPostsSortedByNewest() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByCreatedAtDesc("national", pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getPostsByWall("national", pageable, SortBy.NEWEST);

            // Assert
            assertNotNull(result);
            verify(postRepository).findByWallOrderByCreatedAtDesc("national", pageable);
        }

        @Test
        @DisplayName("Positive: Should get campus posts sorted by OLDEST")
        void shouldGetCampusPostsSortedByOldest() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByCreatedAtAsc("campus", pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getPostsByWall("campus", pageable, SortBy.OLDEST);

            // Assert
            assertNotNull(result);
            verify(postRepository).findByWallOrderByCreatedAtAsc("campus", pageable);
        }

        @Test
        @DisplayName("Positive: Should get posts sorted by MOST_LIKED")
        void shouldGetPostsSortedByMostLiked() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByLikeCountDesc("national", pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getPostsByWall("national", pageable, SortBy.MOST_LIKED);

            // Assert
            assertNotNull(result);
            verify(postRepository).findByWallOrderByLikeCountDesc("national", pageable);
        }

        @Test
        @DisplayName("Positive: Should get posts sorted by LEAST_LIKED")
        void shouldGetPostsSortedByLeastLiked() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByLikeCountAsc("campus", pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getPostsByWall("campus", pageable, SortBy.LEAST_LIKED);

            // Assert
            assertNotNull(result);
            verify(postRepository).findByWallOrderByLikeCountAsc("campus", pageable);
        }

        @Test
        @DisplayName("Positive: Should get all posts when wall is null")
        void shouldGetAllPostsWhenWallIsNull() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getPostsByWall(null, pageable, SortBy.NEWEST);

            // Assert
            assertNotNull(result);
            verify(postRepository).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Edge: Should default to NEWEST when sortBy is null")
        void shouldDefaultToNewestWhenSortByIsNull() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByCreatedAtDesc("national", pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = service.getPostsByWall("national", pageable, null);

            // Assert
            assertNotNull(result);
            verify(postRepository).findByWallOrderByCreatedAtDesc("national", pageable);
        }

        @Test
        @DisplayName("Negative: Should throw exception for invalid wall type")
        void shouldThrowExceptionForInvalidWallType() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getPostsByWall("invalid", pageable, SortBy.NEWEST)
            );
            assertTrue(exception.getMessage().contains("Wall must be"));
        }

        @Test
        @DisplayName("Edge: Should accept 'national' wall type")
        void shouldAcceptNationalWallType() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByCreatedAtDesc("national", pageable)).thenReturn(mockPage);

            // Act & Assert
            assertDoesNotThrow(() -> service.getPostsByWall("national", pageable, SortBy.NEWEST));
        }

        @Test
        @DisplayName("Edge: Should accept 'campus' wall type")
        void shouldAcceptCampusWallType() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByCreatedAtDesc("campus", pageable)).thenReturn(mockPage);

            // Act & Assert
            assertDoesNotThrow(() -> service.getPostsByWall("campus", pageable, SortBy.NEWEST));
        }
    }
}
