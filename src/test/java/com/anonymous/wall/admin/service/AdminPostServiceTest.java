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

@DisplayName("AdminPostService Tests")
class AdminPostServiceTest {

    private AdminPostServiceImpl adminPostService;
    private PostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        adminPostService = new AdminPostServiceImpl();
        
        try {
            var repoField = AdminPostServiceImpl.class.getDeclaredField("postRepository");
            repoField.setAccessible(true);
            repoField.set(adminPostService, postRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Get All Posts - No Filters Cases")
    class GetAllPostsNoFiltersCases {

        @Test
        @DisplayName("Should get all posts without filters and sorting")
        void shouldGetAllPostsWithoutFiltersAndSorting() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, null, null);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAll(pageable);
            verify(postRepository, never()).findAllOrderByCreatedAtDesc(any());
        }

        @Test
        @DisplayName("Should sort by createdAt descending")
        void shouldSortByCreatedAtDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "createdAt", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByCreatedAtDesc(pageable);
            verify(postRepository, never()).findAll(any());
        }

        @Test
        @DisplayName("Should sort by createdAt ascending")
        void shouldSortByCreatedAtAsc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "createdAt", "asc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("Should sort by likeCount descending")
        void shouldSortByLikeCountDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByLikeCountDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "likeCount", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByLikeCountDesc(pageable);
        }

        @Test
        @DisplayName("Should sort by likeCount ascending")
        void shouldSortByLikeCountAsc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByLikeCountAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "likeCount", "asc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByLikeCountAsc(pageable);
        }

        @Test
        @DisplayName("Should sort by commentCount descending")
        void shouldSortByCommentCountDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCommentCountDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "commentCount", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByCommentCountDesc(pageable);
        }

        @Test
        @DisplayName("Should sort by commentCount ascending")
        void shouldSortByCommentCountAsc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCommentCountAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "commentCount", "asc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByCommentCountAsc(pageable);
        }

        @Test
        @DisplayName("Should sort by userId descending")
        void shouldSortByUserIdDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByUserIdDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "userId", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByUserIdDesc(pageable);
        }

        @Test
        @DisplayName("Should sort by userId ascending")
        void shouldSortByUserIdAsc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByUserIdAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "userId", "asc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByUserIdAsc(pageable);
        }

        @Test
        @DisplayName("Should sort by author descending")
        void shouldSortByAuthorDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByUserIdDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "author", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByUserIdDesc(pageable);
        }

        @Test
        @DisplayName("Should handle case-insensitive sortBy")
        void shouldHandleCaseInsensitiveSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "CREATEDAT", "DESC");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Should use default sorting for unknown sortBy")
        void shouldUseDefaultForUnknownSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "invalidField", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should default to desc when sortOrder is null")
        void shouldDefaultToDescWhenSortOrderNull() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "createdAt", null);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Should handle empty sortOrder string")
        void shouldHandleEmptySortOrder() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "createdAt", "");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByCreatedAtAsc(pageable);
        }
    }

    @Nested
    @DisplayName("Get All Posts - Filter Cases")
    class GetAllPostsFilterCases {

        @Test
        @DisplayName("Should filter by hidden status true")
        void shouldFilterByHiddenTrue() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByHidden(true, pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, true, null, null);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findByHidden(true, pageable);
            verify(postRepository, never()).findAll(any());
        }

        @Test
        @DisplayName("Should filter by hidden status false")
        void shouldFilterByHiddenFalse() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByHidden(false, pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, false, null, null);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findByHidden(false, pageable);
        }

        @Test
        @DisplayName("Should filter by userId")
        void shouldFilterByUserId() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByUserId(userId, pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, userId, null, null, null, null);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findByUserId(userId, pageable);
        }

        @Test
        @DisplayName("Should filter by userId and hidden status")
        void shouldFilterByUserIdAndHidden() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByUserIdAndHidden(userId, true, pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, userId, null, true, null, null);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findByUserIdAndHidden(userId, true, pageable);
        }

        @Test
        @DisplayName("Should ignore sortBy when filters are applied")
        void shouldIgnoreSortByWhenFiltersApplied() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByUserId(userId, pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, userId, null, null, "likeCount", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findByUserId(userId, pageable);
            verify(postRepository, never()).findAllOrderByLikeCountDesc(any());
        }
    }

    @Nested
    @DisplayName("Get Posts By Wall - Positive Cases")
    class GetPostsByWallPositiveCases {

        @Test
        @DisplayName("Should get national wall posts with newest sort")
        void shouldGetNationalWallPostsNewest() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByCreatedAtDesc("national", pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getPostsByWall("national", pageable, SortBy.NEWEST);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findByWallOrderByCreatedAtDesc("national", pageable);
        }

        @Test
        @DisplayName("Should get campus wall posts with oldest sort")
        void shouldGetCampusWallPostsOldest() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByCreatedAtAsc("campus", pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getPostsByWall("campus", pageable, SortBy.OLDEST);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findByWallOrderByCreatedAtAsc("campus", pageable);
        }

        @Test
        @DisplayName("Should get national wall posts with most liked sort")
        void shouldGetNationalWallPostsMostLiked() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByLikeCountDesc("national", pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getPostsByWall("national", pageable, SortBy.MOST_LIKED);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findByWallOrderByLikeCountDesc("national", pageable);
        }

        @Test
        @DisplayName("Should get campus wall posts with least liked sort")
        void shouldGetCampusWallPostsLeastLiked() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByLikeCountAsc("campus", pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getPostsByWall("campus", pageable, SortBy.LEAST_LIKED);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findByWallOrderByLikeCountAsc("campus", pageable);
        }

        @Test
        @DisplayName("Should get all posts when wall is null with newest sort")
        void shouldGetAllPostsWhenWallNull() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getPostsByWall(null, pageable, SortBy.NEWEST);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Should get all posts when wall is null with oldest sort")
        void shouldGetAllPostsWhenWallNullOldest() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getPostsByWall(null, pageable, SortBy.OLDEST);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("Should get all posts when wall is null with most liked sort")
        void shouldGetAllPostsWhenWallNullMostLiked() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByLikeCountDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getPostsByWall(null, pageable, SortBy.MOST_LIKED);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByLikeCountDesc(pageable);
        }

        @Test
        @DisplayName("Should get all posts when wall is null with least liked sort")
        void shouldGetAllPostsWhenWallNullLeastLiked() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByLikeCountAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getPostsByWall(null, pageable, SortBy.LEAST_LIKED);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByLikeCountAsc(pageable);
        }

        @Test
        @DisplayName("Should default to newest sort when sortBy is null")
        void shouldDefaultToNewestWhenSortByNull() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findByWallOrderByCreatedAtDesc("national", pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getPostsByWall("national", pageable, null);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findByWallOrderByCreatedAtDesc("national", pageable);
        }
    }

    @Nested
    @DisplayName("Get Posts By Wall - Negative Cases")
    class GetPostsByWallNegativeCases {

        @Test
        @DisplayName("Should throw exception for invalid wall type")
        void shouldThrowForInvalidWallType() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminPostService.getPostsByWall("invalid", pageable, SortBy.NEWEST));
            assertTrue(exception.getMessage().contains("Wall must be"));
        }

        @Test
        @DisplayName("Should throw exception for empty wall string")
        void shouldThrowForEmptyWallString() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminPostService.getPostsByWall("", pageable, SortBy.NEWEST));
            assertTrue(exception.getMessage().contains("Wall must be"));
        }

        @Test
        @DisplayName("Should throw exception for whitespace wall string")
        void shouldThrowForWhitespaceWallString() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminPostService.getPostsByWall("   ", pageable, SortBy.NEWEST));
            assertTrue(exception.getMessage().contains("Wall must be"));
        }
    }

    @Nested
    @DisplayName("Delete Post - Positive Cases")
    class DeletePostPositiveCases {

        @Test
        @DisplayName("Should soft delete post successfully")
        void shouldSoftDeletePost() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Post post = createTestPost(postId);
            post.setHidden(false);
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.update(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminPostService.deletePost(postId);

            // Assert
            assertTrue(post.isHidden());
            verify(postRepository, times(1)).findById(postId);
            verify(postRepository, times(1)).update(post);
        }

        @Test
        @DisplayName("Should soft delete already hidden post without error")
        void shouldSoftDeleteAlreadyHiddenPost() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Post post = createTestPost(postId);
            post.setHidden(true);
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.update(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminPostService.deletePost(postId);

            // Assert
            assertTrue(post.isHidden());
            verify(postRepository, times(1)).update(post);
        }
    }

    @Nested
    @DisplayName("Delete Post - Negative Cases")
    class DeletePostNegativeCases {

        @Test
        @DisplayName("Should throw exception for non-existent post")
        void shouldThrowForNonExistentPost() {
            // Arrange
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminPostService.deletePost(postId));
            assertTrue(exception.getMessage().contains("Post not found"));
            verify(postRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle different page sizes")
        void shouldHandleDifferentPageSizes() {
            // Arrange
            Pageable pageable = Pageable.from(0, 100);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, null, null);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle large page numbers")
        void shouldHandleLargePageNumbers() {
            // Arrange
            Pageable pageable = Pageable.from(100, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, null, null);

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle multiple delete operations on same post")
        void shouldHandleMultipleDeletes() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Post post = createTestPost(postId);
            post.setHidden(false);
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.update(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminPostService.deletePost(postId);
            assertTrue(post.isHidden());
            
            adminPostService.deletePost(postId);
            assertTrue(post.isHidden());

            // Assert
            verify(postRepository, times(2)).findById(postId);
            verify(postRepository, times(2)).update(post);
        }

        @Test
        @DisplayName("Should handle mixed case sortBy values")
        void shouldHandleMixedCaseSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByLikeCountDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "LiKeCoUnT", "desc");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByLikeCountDesc(pageable);
        }

        @Test
        @DisplayName("Should handle mixed case sortOrder values")
        void shouldHandleMixedCaseSortOrder() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Post> mockPage = mock(Page.class);
            when(postRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Post> result = adminPostService.getAllPosts(pageable, null, null, null, "createdAt", "AsC");

            // Assert
            assertNotNull(result);
            verify(postRepository, times(1)).findAllOrderByCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("Should handle posts with all counters at zero")
        void shouldHandlePostsWithZeroCounters() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Post post = createTestPost(postId);
            post.setLikeCount(0);
            post.setCommentCount(0);
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.update(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminPostService.deletePost(postId);

            // Assert
            assertTrue(post.isHidden());
            assertEquals(0, post.getLikeCount());
            assertEquals(0, post.getCommentCount());
        }

        @Test
        @DisplayName("Should handle posts with high counters")
        void shouldHandlePostsWithHighCounters() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Post post = createTestPost(postId);
            post.setLikeCount(999999);
            post.setCommentCount(999999);
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.update(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminPostService.deletePost(postId);

            // Assert
            assertTrue(post.isHidden());
            assertEquals(999999, post.getLikeCount());
            assertEquals(999999, post.getCommentCount());
        }
    }

    private Post createTestPost(UUID postId) {
        Post post = new Post();
        post.setId(postId);
        post.setUserId(UUID.randomUUID());
        post.setTitle("Test Post");
        post.setContent("Test content");
        post.setWall("campus");
        post.setSchoolDomain("harvard.edu");
        post.setHidden(false);
        post.setLikeCount(10);
        post.setCommentCount(5);
        post.setCreatedAt(OffsetDateTime.now());
        post.setUpdatedAt(OffsetDateTime.now());
        return post;
    }
}
