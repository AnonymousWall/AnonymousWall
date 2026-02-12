package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.CommentReport;
import com.anonymous.wall.entity.PostReport;
import com.anonymous.wall.repository.CommentReportRepository;
import com.anonymous.wall.repository.PostReportRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AdminReportServiceImpl Tests")
class AdminReportServiceImplTest {

    private AdminReportServiceImpl service;
    private PostReportRepository postReportRepository;
    private CommentReportRepository commentReportRepository;

    @BeforeEach
    void setUp() {
        postReportRepository = mock(PostReportRepository.class);
        commentReportRepository = mock(CommentReportRepository.class);
        service = new AdminReportServiceImpl();
        
        // Inject mocks via reflection
        try {
            var postRepoField = AdminReportServiceImpl.class.getDeclaredField("postReportRepository");
            postRepoField.setAccessible(true);
            postRepoField.set(service, postReportRepository);
            
            var commentRepoField = AdminReportServiceImpl.class.getDeclaredField("commentReportRepository");
            commentRepoField.setAccessible(true);
            commentRepoField.set(service, commentReportRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject repositories", e);
        }
    }

    private PostReport createMockPostReport(UUID id, UUID postId, UUID reporterUserId, String reason) {
        PostReport report = new PostReport();
        report.setId(id);
        report.setPostId(postId);
        report.setReporterUserId(reporterUserId);
        report.setReason(reason);
        report.setCreatedAt(OffsetDateTime.now());
        return report;
    }

    private CommentReport createMockCommentReport(UUID id, UUID commentId, UUID reporterUserId, String reason) {
        CommentReport report = new CommentReport();
        report.setId(id);
        report.setCommentId(commentId);
        report.setReporterUserId(reporterUserId);
        report.setReason(reason);
        report.setCreatedAt(OffsetDateTime.now());
        return report;
    }

    @Nested
    @DisplayName("GetAllPostReports Tests")
    class GetAllPostReportsTests {

        @Test
        @DisplayName("Positive: Should return all post reports with pagination")
        void shouldReturnAllPostReportsWithPagination() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<PostReport> mockPage = mock(Page.class);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = service.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            verify(postReportRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Positive: Should handle different page sizes")
        void shouldHandleDifferentPageSizes() {
            // Arrange
            Pageable pageable1 = Pageable.from(0, 10);
            Pageable pageable2 = Pageable.from(0, 50);
            Page<PostReport> mockPage1 = mock(Page.class);
            Page<PostReport> mockPage2 = mock(Page.class);
            when(postReportRepository.findAll(pageable1)).thenReturn(mockPage1);
            when(postReportRepository.findAll(pageable2)).thenReturn(mockPage2);

            // Act
            Page<PostReport> result1 = service.getAllPostReports(pageable1);
            Page<PostReport> result2 = service.getAllPostReports(pageable2);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            verify(postReportRepository).findAll(pageable1);
            verify(postReportRepository).findAll(pageable2);
        }

        @Test
        @DisplayName("Positive: Should handle different page numbers")
        void shouldHandleDifferentPageNumbers() {
            // Arrange
            Pageable page0 = Pageable.from(0, 10);
            Pageable page1 = Pageable.from(1, 10);
            Page<PostReport> mockPage = mock(Page.class);
            when(postReportRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

            // Act
            Page<PostReport> result1 = service.getAllPostReports(page0);
            Page<PostReport> result2 = service.getAllPostReports(page1);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            verify(postReportRepository).findAll(page0);
            verify(postReportRepository).findAll(page1);
        }

        @Test
        @DisplayName("Edge: Should handle empty result set")
        void shouldHandleEmptyResultSet() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<PostReport> emptyPage = Page.empty();
            when(postReportRepository.findAll(pageable)).thenReturn(emptyPage);

            // Act
            Page<PostReport> result = service.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.getContent().size());
        }

        @Test
        @DisplayName("Edge: Should handle very large page size")
        void shouldHandleVeryLargePageSize() {
            // Arrange
            Pageable pageable = Pageable.from(0, 1000);
            Page<PostReport> mockPage = mock(Page.class);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = service.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            verify(postReportRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("GetAllCommentReports Tests")
    class GetAllCommentReportsTests {

        @Test
        @DisplayName("Positive: Should return all comment reports with pagination")
        void shouldReturnAllCommentReportsWithPagination() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<CommentReport> mockPage = mock(Page.class);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = service.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            verify(commentReportRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Positive: Should handle different page sizes")
        void shouldHandleDifferentPageSizes() {
            // Arrange
            Pageable pageable1 = Pageable.from(0, 10);
            Pageable pageable2 = Pageable.from(0, 50);
            Page<CommentReport> mockPage1 = mock(Page.class);
            Page<CommentReport> mockPage2 = mock(Page.class);
            when(commentReportRepository.findAll(pageable1)).thenReturn(mockPage1);
            when(commentReportRepository.findAll(pageable2)).thenReturn(mockPage2);

            // Act
            Page<CommentReport> result1 = service.getAllCommentReports(pageable1);
            Page<CommentReport> result2 = service.getAllCommentReports(pageable2);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            verify(commentReportRepository).findAll(pageable1);
            verify(commentReportRepository).findAll(pageable2);
        }

        @Test
        @DisplayName("Positive: Should handle different page numbers")
        void shouldHandleDifferentPageNumbers() {
            // Arrange
            Pageable page0 = Pageable.from(0, 10);
            Pageable page1 = Pageable.from(1, 10);
            Page<CommentReport> mockPage = mock(Page.class);
            when(commentReportRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

            // Act
            Page<CommentReport> result1 = service.getAllCommentReports(page0);
            Page<CommentReport> result2 = service.getAllCommentReports(page1);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            verify(commentReportRepository).findAll(page0);
            verify(commentReportRepository).findAll(page1);
        }

        @Test
        @DisplayName("Edge: Should handle empty result set")
        void shouldHandleEmptyResultSet() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<CommentReport> emptyPage = Page.empty();
            when(commentReportRepository.findAll(pageable)).thenReturn(emptyPage);

            // Act
            Page<CommentReport> result = service.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.getContent().size());
        }

        @Test
        @DisplayName("Edge: Should handle very large page size")
        void shouldHandleVeryLargePageSize() {
            // Arrange
            Pageable pageable = Pageable.from(0, 1000);
            Page<CommentReport> mockPage = mock(Page.class);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = service.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            verify(commentReportRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Positive: Should independently manage post and comment reports")
        void shouldIndependentlyManagePostAndCommentReports() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<PostReport> postPage = mock(Page.class);
            Page<CommentReport> commentPage = mock(Page.class);
            when(postReportRepository.findAll(pageable)).thenReturn(postPage);
            when(commentReportRepository.findAll(pageable)).thenReturn(commentPage);

            // Act
            Page<PostReport> postResult = service.getAllPostReports(pageable);
            Page<CommentReport> commentResult = service.getAllCommentReports(pageable);

            // Assert
            assertNotNull(postResult);
            assertNotNull(commentResult);
            verify(postReportRepository).findAll(pageable);
            verify(commentReportRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Positive: Should not interfere between post and comment report calls")
        void shouldNotInterfereBetweenPostAndCommentReportCalls() {
            // Arrange
            Pageable pageable1 = Pageable.from(0, 10);
            Pageable pageable2 = Pageable.from(1, 20);
            Page<PostReport> postPage = mock(Page.class);
            Page<CommentReport> commentPage = mock(Page.class);
            when(postReportRepository.findAll(any(Pageable.class))).thenReturn(postPage);
            when(commentReportRepository.findAll(any(Pageable.class))).thenReturn(commentPage);

            // Act
            service.getAllPostReports(pageable1);
            service.getAllCommentReports(pageable2);
            service.getAllPostReports(pageable2);
            service.getAllCommentReports(pageable1);

            // Assert
            verify(postReportRepository, times(2)).findAll(any(Pageable.class));
            verify(commentReportRepository, times(2)).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Edge: Should handle page number 0")
        void shouldHandlePageNumberZero() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<PostReport> mockPage = mock(Page.class);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = service.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            verify(postReportRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Edge: Should handle very small page size")
        void shouldHandleVerySmallPageSize() {
            // Arrange
            Pageable pageable = Pageable.from(0, 1);
            Page<CommentReport> mockPage = mock(Page.class);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = service.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            verify(commentReportRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Edge: Should handle repository exception gracefully")
        void shouldHandleRepositoryExceptionGracefully() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            when(postReportRepository.findAll(pageable)).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> service.getAllPostReports(pageable));
        }
    }
}
