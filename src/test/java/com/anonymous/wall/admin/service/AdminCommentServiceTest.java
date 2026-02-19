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

@DisplayName("AdminCommentService Tests")
class AdminCommentServiceTest {

    private AdminCommentServiceImpl adminCommentService;
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        adminCommentService = new AdminCommentServiceImpl();
        
        try {
            var repoField = AdminCommentServiceImpl.class.getDeclaredField("commentRepository");
            repoField.setAccessible(true);
            repoField.set(adminCommentService, commentRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Get All Comments - No Filters Cases")
    class GetAllCommentsNoFiltersCases {

        @Test
        @DisplayName("Should get all comments without filters and sorting")
        void shouldGetAllCommentsWithoutFiltersAndSorting() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAll(pageable);
            verify(commentRepository, never()).findAllOrderByCreatedAtDesc(any());
        }

        @Test
        @DisplayName("Should sort by createdAt descending")
        void shouldSortByCreatedAtDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "createdAt", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByCreatedAtDesc(pageable);
            verify(commentRepository, never()).findAll(any());
        }

        @Test
        @DisplayName("Should sort by createdAt ascending")
        void shouldSortByCreatedAtAsc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "createdAt", "asc");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("Should sort by userId descending")
        void shouldSortByUserIdDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByUserIdDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "userId", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByUserIdDesc(pageable);
        }

        @Test
        @DisplayName("Should sort by userId ascending")
        void shouldSortByUserIdAsc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByUserIdAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "userId", "asc");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByUserIdAsc(pageable);
        }

        @Test
        @DisplayName("Should sort by author descending")
        void shouldSortByAuthorDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByUserIdDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "author", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByUserIdDesc(pageable);
        }

        @Test
        @DisplayName("Should sort by author ascending")
        void shouldSortByAuthorAsc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByUserIdAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "author", "asc");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByUserIdAsc(pageable);
        }

        @Test
        @DisplayName("Should handle case-insensitive sortBy")
        void shouldHandleCaseInsensitiveSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "CREATEDAT", "DESC");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Should use default sorting for unknown sortBy")
        void shouldUseDefaultForUnknownSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "invalidField", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should default to desc when sortOrder is null")
        void shouldDefaultToDescWhenSortOrderNull() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "createdAt", null);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Should handle empty sortOrder string")
        void shouldHandleEmptySortOrder() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "createdAt", "");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByCreatedAtAsc(pageable);
        }
    }

    @Nested
    @DisplayName("Get All Comments - Filter Cases")
    class GetAllCommentsFilterCases {

        @Test
        @DisplayName("Should filter by hidden status true")
        void shouldFilterByHiddenTrue() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByHidden(true, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, true, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findByHidden(true, pageable);
            verify(commentRepository, never()).findAll(any());
        }

        @Test
        @DisplayName("Should filter by hidden status false")
        void shouldFilterByHiddenFalse() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByHidden(false, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, false, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findByHidden(false, pageable);
        }

        @Test
        @DisplayName("Should filter by userId")
        void shouldFilterByUserId() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserId(userId, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, userId, null, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findByUserId(userId, pageable);
        }

        @Test
        @DisplayName("Should filter by userId and hidden status")
        void shouldFilterByUserIdAndHidden() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserIdAndHidden(userId, true, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, userId, true, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findByUserIdAndHidden(userId, true, pageable);
        }

        @Test
        @DisplayName("Should ignore sortBy when filters are applied")
        void shouldIgnoreSortByWhenFiltersApplied() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserId(userId, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, userId, null, "createdAt", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findByUserId(userId, pageable);
            verify(commentRepository, never()).findAllOrderByCreatedAtDesc(any());
        }
    }

    @Nested
    @DisplayName("Delete Comment - Positive Cases")
    class DeleteCommentPositiveCases {

        @Test
        @DisplayName("Should soft delete comment successfully")
        void shouldSoftDeleteComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = createTestComment(commentId);
            comment.setHidden(false);
            
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.update(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminCommentService.deleteComment(commentId);

            // Assert
            assertTrue(comment.isHidden());
            verify(commentRepository, times(1)).findById(commentId);
            verify(commentRepository, times(1)).update(comment);
        }

        @Test
        @DisplayName("Should soft delete already hidden comment without error")
        void shouldSoftDeleteAlreadyHiddenComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = createTestComment(commentId);
            comment.setHidden(true);
            
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.update(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminCommentService.deleteComment(commentId);

            // Assert
            assertTrue(comment.isHidden());
            verify(commentRepository, times(1)).update(comment);
        }
    }

    @Nested
    @DisplayName("Delete Comment - Negative Cases")
    class DeleteCommentNegativeCases {

        @Test
        @DisplayName("Should throw exception for non-existent comment")
        void shouldThrowForNonExistentComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminCommentService.deleteComment(commentId));
            assertTrue(exception.getMessage().contains("Comment not found"));
            verify(commentRepository, never()).update(any());
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
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle large page numbers")
        void shouldHandleLargePageNumbers() {
            // Arrange
            Pageable pageable = Pageable.from(100, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle multiple delete operations on same comment")
        void shouldHandleMultipleDeletes() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = createTestComment(commentId);
            comment.setHidden(false);
            
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.update(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminCommentService.deleteComment(commentId);
            assertTrue(comment.isHidden());
            
            adminCommentService.deleteComment(commentId);
            assertTrue(comment.isHidden());

            // Assert
            verify(commentRepository, times(2)).findById(commentId);
            verify(commentRepository, times(2)).update(comment);
        }

        @Test
        @DisplayName("Should handle mixed case sortBy values")
        void shouldHandleMixedCaseSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByUserIdDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "UsErId", "desc");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByUserIdDesc(pageable);
        }

        @Test
        @DisplayName("Should handle mixed case sortOrder values")
        void shouldHandleMixedCaseSortOrder() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "createdAt", "AsC");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("Should handle comments with long text")
        void shouldHandleCommentsWithLongText() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = createTestComment(commentId);
            String longText = "A".repeat(10000);
            comment.setText(longText);
            
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.update(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminCommentService.deleteComment(commentId);

            // Assert
            assertTrue(comment.isHidden());
            assertEquals(longText, comment.getText());
        }

        @Test
        @DisplayName("Should handle comments with empty text")
        void shouldHandleCommentsWithEmptyText() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = createTestComment(commentId);
            comment.setText("");
            
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.update(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminCommentService.deleteComment(commentId);

            // Assert
            assertTrue(comment.isHidden());
            assertEquals("", comment.getText());
        }

        @Test
        @DisplayName("Should handle filtering with same userId for multiple operations")
        void shouldHandleFilteringWithSameUserId() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserId(userId, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result1 = adminCommentService.getAllComments(pageable, userId, null, null, null);
            Page<Comment> result2 = adminCommentService.getAllComments(pageable, userId, null, null, null);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            verify(commentRepository, times(2)).findByUserId(userId, pageable);
        }

        @Test
        @DisplayName("Should handle sorting combinations that don't exist")
        void shouldHandleSortingCombinationsThatDontExist() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "nonexistent", "asc");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle special characters in sort order")
        void shouldHandleSpecialCharactersInSortOrder() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act - Special characters should not match "desc", so it becomes "asc"
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, null, "createdAt", "!@#");

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findAllOrderByCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("Should handle null userId and non-null hidden filter")
        void shouldHandleNullUserIdAndNonNullHidden() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByHidden(false, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, null, false, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findByHidden(false, pageable);
            verify(commentRepository, never()).findAll(any());
        }

        @Test
        @DisplayName("Should handle non-null userId and null hidden filter")
        void shouldHandleNonNullUserIdAndNullHidden() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserId(userId, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, userId, null, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findByUserId(userId, pageable);
            verify(commentRepository, never()).findAll(any());
        }

        @Test
        @DisplayName("Should handle both userId and hidden filters with false")
        void shouldHandleBothFiltersWithFalse() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserIdAndHidden(userId, false, pageable)).thenReturn(mockPage);

            // Act
            Page<Comment> result = adminCommentService.getAllComments(pageable, userId, false, null, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).findByUserIdAndHidden(userId, false, pageable);
        }
    }

    private Comment createTestComment(UUID commentId) {
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setParentId(UUID.randomUUID());
        comment.setParentType("POST");
        comment.setUserId(UUID.randomUUID());
        comment.setText("Test comment");
        comment.setProfileName("Anonymous");
        comment.setHidden(false);
        comment.setCreatedAt(OffsetDateTime.now());
        return comment;
    }
}
