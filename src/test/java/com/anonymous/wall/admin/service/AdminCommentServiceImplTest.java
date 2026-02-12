package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.repository.CommentRepository;
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

@DisplayName("AdminCommentServiceImpl Tests")
class AdminCommentServiceImplTest {

    private AdminCommentServiceImpl service;
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        service = new AdminCommentServiceImpl();
        
        // Inject mock via reflection
        try {
            var repoField = AdminCommentServiceImpl.class.getDeclaredField("commentRepository");
            repoField.setAccessible(true);
            repoField.set(service, commentRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject repository", e);
        }
    }

    private Comment createMockComment(UUID id, UUID postId, UUID userId, String text, boolean hidden) {
        Comment comment = new Comment(postId, userId, text);
        comment.setId(id);
        comment.setHidden(hidden);
        comment.setCreatedAt(OffsetDateTime.now());
        comment.setProfileName("Test User");
        return comment;
    }

    @Nested
    @DisplayName("GetAllComments Tests - No Filters")
    class GetAllCommentsNoFiltersTests {

        @Test
        @DisplayName("Positive: Should return all comments with default pagination")
        void shouldReturnAllCommentsWithDefaultPagination() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, null, null, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by createdAt descending")
        void shouldSortByCreatedAtDescending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, null, null, "createdAt", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by createdAt ascending")
        void shouldSortByCreatedAtAscending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, null, null, "createdAt", "asc");

            // Assert
            assertNotNull(result);
            verify(commentRepository).findAllOrderByCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by userId descending")
        void shouldSortByUserIdDescending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByUserIdDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, null, null, "userId", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository).findAllOrderByUserIdDesc(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by userId ascending")
        void shouldSortByUserIdAscending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByUserIdAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, null, null, "userId", "asc");

            // Assert
            assertNotNull(result);
            verify(commentRepository).findAllOrderByUserIdAsc(pageable);
        }

        @Test
        @DisplayName("Edge: Should handle 'author' as alias for userId")
        void shouldHandleAuthorAsAlias() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByUserIdDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, null, null, "author", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository).findAllOrderByUserIdDesc(pageable);
        }

        @Test
        @DisplayName("Edge: Should default to desc when sortOrder is null")
        void shouldDefaultToDescWhenSortOrderIsNull() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, null, null, "createdAt", null);

            // Assert
            assertNotNull(result);
            verify(commentRepository).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Edge: Should be case-insensitive for sortBy")
        void shouldBeCaseInsensitiveForSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, null, null, "CREATEDAT", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Edge: Should be case-insensitive for sortOrder")
        void shouldBeCaseInsensitiveForSortOrder() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, null, null, "createdAt", "ASC");

            // Assert
            assertNotNull(result);
            verify(commentRepository).findAllOrderByCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("Negative: Should fallback to findAll for invalid sortBy")
        void shouldFallbackToFindAllForInvalidSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, null, null, "invalid", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("GetAllComments Tests - With Filters")
    class GetAllCommentsWithFiltersTests {

        @Test
        @DisplayName("Positive: Should filter by hidden status only")
        void shouldFilterByHiddenStatusOnly() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByHidden(true, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, null, true, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository).findByHidden(true, pageable);
        }

        @Test
        @DisplayName("Positive: Should filter by userId only")
        void shouldFilterByUserIdOnly() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserId(userId, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, userId, null, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository).findByUserId(userId, pageable);
        }

        @Test
        @DisplayName("Positive: Should filter by both userId and hidden")
        void shouldFilterByBothUserIdAndHidden() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserIdAndHidden(userId, false, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, userId, false, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository).findByUserIdAndHidden(userId, false, pageable);
        }

        @Test
        @DisplayName("Edge: Should ignore sortBy when filters are present")
        void shouldIgnoreSortByWhenFiltersArePresent() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserId(userId, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getAllComments(pageable, userId, null, "createdAt", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository).findByUserId(userId, pageable);
            // sortBy should be ignored (logged as warning)
        }
    }

    @Nested
    @DisplayName("DeleteComment Tests")
    class DeleteCommentTests {

        @Test
        @DisplayName("Positive: Should soft-delete comment by setting hidden to true")
        void shouldSoftDeleteComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = createMockComment(commentId, UUID.randomUUID(), UUID.randomUUID(), "Test", false);
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.update(any(Comment.class))).thenReturn(comment);

            // Act
            service.deleteComment(commentId);

            // Assert
            assertTrue(comment.isHidden());
            verify(commentRepository).findById(commentId);
            verify(commentRepository).update(argThat(c -> c.isHidden()));
        }

        @Test
        @DisplayName("Negative: Should throw exception for non-existent comment")
        void shouldThrowExceptionForNonExistentComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteComment(commentId)
            );
            assertTrue(exception.getMessage().contains("Comment not found"));
            verify(commentRepository).findById(commentId);
            verify(commentRepository, never()).update(any());
        }

        @Test
        @DisplayName("Edge: Should handle already hidden comment")
        void shouldHandleAlreadyHiddenComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = createMockComment(commentId, UUID.randomUUID(), UUID.randomUUID(), "Test", true);
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.update(any(Comment.class))).thenReturn(comment);

            // Act
            service.deleteComment(commentId);

            // Assert
            assertTrue(comment.isHidden());
            verify(commentRepository).findById(commentId);
            verify(commentRepository).update(comment);
        }

        @Test
        @DisplayName("Edge: Should handle null UUID gracefully")
        void shouldHandleNullUuidGracefully() {
            // Arrange
            when(commentRepository.findById(null)).thenThrow(new IllegalArgumentException("ID cannot be null"));

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> service.deleteComment(null));
        }
    }

    @Nested
    @DisplayName("Pagination Tests")
    class PaginationTests {

        @Test
        @DisplayName("Positive: Should handle different page sizes")
        void shouldHandleDifferentPageSizes() {
            // Arrange
            Pageable pageable1 = Pageable.from(0, 10);
            Pageable pageable2 = Pageable.from(0, 50);
            Page<Comment> mockPage1 = mock(Page.class);
            Page<Comment> mockPage2 = mock(Page.class);
            when(commentRepository.findAll(pageable1)).thenReturn(mockPage1);
            when(commentRepository.findAll(pageable2)).thenReturn(mockPage2);

            // Act
            Page<Comment> result1 = service.getAllComments(pageable1, null, null, null, null);
            Page<Comment> result2 = service.getAllComments(pageable2, null, null, null, null);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            verify(commentRepository).findAll(pageable1);
            verify(commentRepository).findAll(pageable2);
        }

        @Test
        @DisplayName("Positive: Should handle different page numbers")
        void shouldHandleDifferentPageNumbers() {
            // Arrange
            Pageable page0 = Pageable.from(0, 10);
            Pageable page1 = Pageable.from(1, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

            // Act
            Page<Comment> result1 = service.getAllComments(page0, null, null, null, null);
            Page<Comment> result2 = service.getAllComments(page1, null, null, null, null);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
        }
    }
}
