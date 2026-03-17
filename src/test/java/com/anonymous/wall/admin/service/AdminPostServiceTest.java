package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.PollOption;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.service.base.PollService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AdminPostService Tests")
class AdminPostServiceTest {

    private AdminPostServiceImpl adminPostService;
    private PostRepository postRepository;
    private PollService pollService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        pollService = mock(PollService.class);
        adminPostService = new AdminPostServiceImpl();
        
        try {
            var repoField = AdminPostServiceImpl.class.getDeclaredField("postRepository");
            repoField.setAccessible(true);
            repoField.set(adminPostService, postRepository);

            var pollServiceField = AdminPostServiceImpl.class.getDeclaredField("pollService");
            pollServiceField.setAccessible(true);
            pollServiceField.set(adminPostService, pollService);
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

    @Nested
    @DisplayName("Get Poll Data Cases")
    class GetPollDataCases {

        @Test
        @DisplayName("Should return poll data for a poll post")
        void shouldReturnPollDataForPollPost() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Post post = createTestPost(postId);
            post.setPostType("poll");
            post.setTotalVotes(5);

            PollOption option1 = new PollOption(postId, "Option A", 0);
            option1.setId(UUID.randomUUID());
            option1.setVoteCount(3);

            PollOption option2 = new PollOption(postId, "Option B", 1);
            option2.setId(UUID.randomUUID());
            option2.setVoteCount(2);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(pollService.getPollOptions(postId)).thenReturn(List.of(option1, option2));

            // Act
            Map<String, Object> result = adminPostService.getPollData(postId);

            // Assert
            assertNotNull(result);
            assertEquals(5, result.get("totalVotes"));
            List<?> options = (List<?>) result.get("options");
            assertNotNull(options);
            assertEquals(2, options.size());

            // Verify first option has voteCount and percentage
            Map<?, ?> firstOption = (Map<?, ?>) options.get(0);
            assertEquals(3, firstOption.get("voteCount"));
            assertEquals("Option A", firstOption.get("optionText"));

            verify(pollService, times(1)).getPollOptions(postId);
        }

        @Test
        @DisplayName("Should return poll data for a hidden poll post")
        void shouldReturnPollDataForHiddenPollPost() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Post post = createTestPost(postId);
            post.setPostType("poll");
            post.setHidden(true);
            post.setTotalVotes(0);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(pollService.getPollOptions(postId)).thenReturn(List.of());

            // Act
            Map<String, Object> result = adminPostService.getPollData(postId);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.get("totalVotes"));
            verify(pollService, times(1)).getPollOptions(postId);
        }

        @Test
        @DisplayName("Should compute percentage correctly")
        void shouldComputePercentageCorrectly() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Post post = createTestPost(postId);
            post.setPostType("poll");
            post.setTotalVotes(4);

            PollOption option1 = new PollOption(postId, "Yes", 0);
            option1.setId(UUID.randomUUID());
            option1.setVoteCount(1);

            PollOption option2 = new PollOption(postId, "No", 1);
            option2.setId(UUID.randomUUID());
            option2.setVoteCount(3);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(pollService.getPollOptions(postId)).thenReturn(List.of(option1, option2));

            // Act
            Map<String, Object> result = adminPostService.getPollData(postId);

            // Assert
            List<?> options = (List<?>) result.get("options");
            Map<?, ?> first = (Map<?, ?>) options.get(0);
            Map<?, ?> second = (Map<?, ?>) options.get(1);
            assertEquals(25.0, first.get("percentage"));
            assertEquals(75.0, second.get("percentage"));
        }

        @Test
        @DisplayName("Should throw when post not found")
        void shouldThrowWhenPostNotFound() {
            // Arrange
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> adminPostService.getPollData(postId));
            assertTrue(ex.getMessage().contains("Post not found"));
            verify(pollService, never()).getPollOptions(any());
        }

        @Test
        @DisplayName("Should throw when post is not a poll")
        void shouldThrowWhenPostIsNotPoll() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Post post = createTestPost(postId);
            post.setPostType("standard");
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> adminPostService.getPollData(postId));
            assertTrue(ex.getMessage().contains("not a poll"));
            verify(pollService, never()).getPollOptions(any());
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
