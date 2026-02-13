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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AdminReportService Tests")
class AdminReportServiceTest {

    private AdminReportServiceImpl adminReportService;
    private PostReportRepository postReportRepository;
    private CommentReportRepository commentReportRepository;

    @BeforeEach
    void setUp() {
        postReportRepository = mock(PostReportRepository.class);
        commentReportRepository = mock(CommentReportRepository.class);
        adminReportService = new AdminReportServiceImpl();
        
        try {
            var postRepoField = AdminReportServiceImpl.class.getDeclaredField("postReportRepository");
            postRepoField.setAccessible(true);
            postRepoField.set(adminReportService, postReportRepository);
            
            var commentRepoField = AdminReportServiceImpl.class.getDeclaredField("commentReportRepository");
            commentRepoField.setAccessible(true);
            commentRepoField.set(adminReportService, commentReportRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Get All Post Reports - Positive Cases")
    class GetAllPostReportsPositiveCases {

        @Test
        @DisplayName("Should get all post reports with default pagination")
        void shouldGetAllPostReportsWithDefaultPagination() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<PostReport> mockPage = mock(Page.class);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            verify(postReportRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should get all post reports with custom page size")
        void shouldGetAllPostReportsWithCustomPageSize() {
            // Arrange
            Pageable pageable = Pageable.from(0, 50);
            Page<PostReport> mockPage = mock(Page.class);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            verify(postReportRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should get all post reports with large page size")
        void shouldGetAllPostReportsWithLargePageSize() {
            // Arrange
            Pageable pageable = Pageable.from(0, 100);
            Page<PostReport> mockPage = mock(Page.class);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            verify(postReportRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should get post reports from different pages")
        void shouldGetPostReportsFromDifferentPages() {
            // Arrange
            Pageable pageable1 = Pageable.from(0, 10);
            Pageable pageable2 = Pageable.from(1, 10);
            Page<PostReport> mockPage1 = mock(Page.class);
            Page<PostReport> mockPage2 = mock(Page.class);
            when(postReportRepository.findAll(pageable1)).thenReturn(mockPage1);
            when(postReportRepository.findAll(pageable2)).thenReturn(mockPage2);

            // Act
            Page<PostReport> result1 = adminReportService.getAllPostReports(pageable1);
            Page<PostReport> result2 = adminReportService.getAllPostReports(pageable2);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            verify(postReportRepository, times(1)).findAll(pageable1);
            verify(postReportRepository, times(1)).findAll(pageable2);
        }

        @Test
        @DisplayName("Should handle empty post reports page")
        void shouldHandleEmptyPostReportsPage() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<PostReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(new ArrayList<>());
            when(mockPage.getTotalSize()).thenReturn(0L);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.getContent().size());
            assertEquals(0L, result.getTotalSize());
        }

        @Test
        @DisplayName("Should handle post reports with content")
        void shouldHandlePostReportsWithContent() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            List<PostReport> reports = List.of(
                createTestPostReport(UUID.randomUUID()),
                createTestPostReport(UUID.randomUUID()),
                createTestPostReport(UUID.randomUUID())
            );
            Page<PostReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(reports);
            when(mockPage.getTotalSize()).thenReturn(3L);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.getContent().size());
            assertEquals(3L, result.getTotalSize());
        }
    }

    @Nested
    @DisplayName("Get All Comment Reports - Positive Cases")
    class GetAllCommentReportsPositiveCases {

        @Test
        @DisplayName("Should get all comment reports with default pagination")
        void shouldGetAllCommentReportsWithDefaultPagination() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<CommentReport> mockPage = mock(Page.class);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            verify(commentReportRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should get all comment reports with custom page size")
        void shouldGetAllCommentReportsWithCustomPageSize() {
            // Arrange
            Pageable pageable = Pageable.from(0, 50);
            Page<CommentReport> mockPage = mock(Page.class);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            verify(commentReportRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should get all comment reports with large page size")
        void shouldGetAllCommentReportsWithLargePageSize() {
            // Arrange
            Pageable pageable = Pageable.from(0, 100);
            Page<CommentReport> mockPage = mock(Page.class);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            verify(commentReportRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should get comment reports from different pages")
        void shouldGetCommentReportsFromDifferentPages() {
            // Arrange
            Pageable pageable1 = Pageable.from(0, 10);
            Pageable pageable2 = Pageable.from(1, 10);
            Page<CommentReport> mockPage1 = mock(Page.class);
            Page<CommentReport> mockPage2 = mock(Page.class);
            when(commentReportRepository.findAll(pageable1)).thenReturn(mockPage1);
            when(commentReportRepository.findAll(pageable2)).thenReturn(mockPage2);

            // Act
            Page<CommentReport> result1 = adminReportService.getAllCommentReports(pageable1);
            Page<CommentReport> result2 = adminReportService.getAllCommentReports(pageable2);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            verify(commentReportRepository, times(1)).findAll(pageable1);
            verify(commentReportRepository, times(1)).findAll(pageable2);
        }

        @Test
        @DisplayName("Should handle empty comment reports page")
        void shouldHandleEmptyCommentReportsPage() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<CommentReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(new ArrayList<>());
            when(mockPage.getTotalSize()).thenReturn(0L);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.getContent().size());
            assertEquals(0L, result.getTotalSize());
        }

        @Test
        @DisplayName("Should handle comment reports with content")
        void shouldHandleCommentReportsWithContent() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            List<CommentReport> reports = List.of(
                createTestCommentReport(UUID.randomUUID()),
                createTestCommentReport(UUID.randomUUID()),
                createTestCommentReport(UUID.randomUUID())
            );
            Page<CommentReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(reports);
            when(mockPage.getTotalSize()).thenReturn(3L);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.getContent().size());
            assertEquals(3L, result.getTotalSize());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle very large page numbers for post reports")
        void shouldHandleVeryLargePageNumbersForPostReports() {
            // Arrange
            Pageable pageable = Pageable.from(1000, 10);
            Page<PostReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(new ArrayList<>());
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            verify(postReportRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle very large page numbers for comment reports")
        void shouldHandleVeryLargePageNumbersForCommentReports() {
            // Arrange
            Pageable pageable = Pageable.from(1000, 10);
            Page<CommentReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(new ArrayList<>());
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            verify(commentReportRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle single item page size for post reports")
        void shouldHandleSingleItemPageSizeForPostReports() {
            // Arrange
            Pageable pageable = Pageable.from(0, 1);
            Page<PostReport> mockPage = mock(Page.class);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            verify(postReportRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle single item page size for comment reports")
        void shouldHandleSingleItemPageSizeForCommentReports() {
            // Arrange
            Pageable pageable = Pageable.from(0, 1);
            Page<CommentReport> mockPage = mock(Page.class);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            verify(commentReportRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle multiple consecutive calls for post reports")
        void shouldHandleMultipleConsecutiveCallsForPostReports() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<PostReport> mockPage = mock(Page.class);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result1 = adminReportService.getAllPostReports(pageable);
            Page<PostReport> result2 = adminReportService.getAllPostReports(pageable);
            Page<PostReport> result3 = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            assertNotNull(result3);
            verify(postReportRepository, times(3)).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle multiple consecutive calls for comment reports")
        void shouldHandleMultipleConsecutiveCallsForCommentReports() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<CommentReport> mockPage = mock(Page.class);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result1 = adminReportService.getAllCommentReports(pageable);
            Page<CommentReport> result2 = adminReportService.getAllCommentReports(pageable);
            Page<CommentReport> result3 = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            assertNotNull(result3);
            verify(commentReportRepository, times(3)).findAll(pageable);
        }

        @Test
        @DisplayName("Should handle post reports with various reasons")
        void shouldHandlePostReportsWithVariousReasons() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            List<PostReport> reports = List.of(
                createTestPostReport(UUID.randomUUID(), "Spam"),
                createTestPostReport(UUID.randomUUID(), "Inappropriate content"),
                createTestPostReport(UUID.randomUUID(), "Harassment"),
                createTestPostReport(UUID.randomUUID(), "Violence"),
                createTestPostReport(UUID.randomUUID(), "Hate speech")
            );
            Page<PostReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(reports);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(5, result.getContent().size());
        }

        @Test
        @DisplayName("Should handle comment reports with various reasons")
        void shouldHandleCommentReportsWithVariousReasons() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            List<CommentReport> reports = List.of(
                createTestCommentReport(UUID.randomUUID(), "Spam"),
                createTestCommentReport(UUID.randomUUID(), "Inappropriate content"),
                createTestCommentReport(UUID.randomUUID(), "Harassment"),
                createTestCommentReport(UUID.randomUUID(), "Violence"),
                createTestCommentReport(UUID.randomUUID(), "Hate speech")
            );
            Page<CommentReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(reports);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(5, result.getContent().size());
        }

        @Test
        @DisplayName("Should handle post reports with same reporter")
        void shouldHandlePostReportsWithSameReporter() {
            // Arrange
            UUID reporterId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            List<PostReport> reports = List.of(
                createTestPostReportWithReporter(UUID.randomUUID(), reporterId),
                createTestPostReportWithReporter(UUID.randomUUID(), reporterId),
                createTestPostReportWithReporter(UUID.randomUUID(), reporterId)
            );
            Page<PostReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(reports);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.getContent().size());
        }

        @Test
        @DisplayName("Should handle comment reports with same reporter")
        void shouldHandleCommentReportsWithSameReporter() {
            // Arrange
            UUID reporterId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            List<CommentReport> reports = List.of(
                createTestCommentReportWithReporter(UUID.randomUUID(), reporterId),
                createTestCommentReportWithReporter(UUID.randomUUID(), reporterId),
                createTestCommentReportWithReporter(UUID.randomUUID(), reporterId)
            );
            Page<CommentReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(reports);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.getContent().size());
        }

        @Test
        @DisplayName("Should handle post reports with long reasons")
        void shouldHandlePostReportsWithLongReasons() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            String longReason = "A".repeat(1000);
            PostReport report = createTestPostReport(UUID.randomUUID(), longReason);
            Page<PostReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(List.of(report));
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(longReason, result.getContent().get(0).getReason());
        }

        @Test
        @DisplayName("Should handle comment reports with long reasons")
        void shouldHandleCommentReportsWithLongReasons() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            String longReason = "A".repeat(1000);
            CommentReport report = createTestCommentReport(UUID.randomUUID(), longReason);
            Page<CommentReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(List.of(report));
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(longReason, result.getContent().get(0).getReason());
        }

        @Test
        @DisplayName("Should handle post reports with empty reasons")
        void shouldHandlePostReportsWithEmptyReasons() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            PostReport report = createTestPostReport(UUID.randomUUID(), "");
            Page<PostReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(List.of(report));
            when(postReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<PostReport> result = adminReportService.getAllPostReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("", result.getContent().get(0).getReason());
        }

        @Test
        @DisplayName("Should handle comment reports with empty reasons")
        void shouldHandleCommentReportsWithEmptyReasons() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            CommentReport report = createTestCommentReport(UUID.randomUUID(), "");
            Page<CommentReport> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(List.of(report));
            when(commentReportRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<CommentReport> result = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("", result.getContent().get(0).getReason());
        }

        @Test
        @DisplayName("Should handle interleaved post and comment report calls")
        void shouldHandleInterleavedPostAndCommentReportCalls() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<PostReport> mockPostPage = mock(Page.class);
            Page<CommentReport> mockCommentPage = mock(Page.class);
            when(postReportRepository.findAll(pageable)).thenReturn(mockPostPage);
            when(commentReportRepository.findAll(pageable)).thenReturn(mockCommentPage);

            // Act
            Page<PostReport> postResult1 = adminReportService.getAllPostReports(pageable);
            Page<CommentReport> commentResult1 = adminReportService.getAllCommentReports(pageable);
            Page<PostReport> postResult2 = adminReportService.getAllPostReports(pageable);
            Page<CommentReport> commentResult2 = adminReportService.getAllCommentReports(pageable);

            // Assert
            assertNotNull(postResult1);
            assertNotNull(commentResult1);
            assertNotNull(postResult2);
            assertNotNull(commentResult2);
            verify(postReportRepository, times(2)).findAll(pageable);
            verify(commentReportRepository, times(2)).findAll(pageable);
        }
    }

    private PostReport createTestPostReport(UUID postId) {
        return createTestPostReport(postId, "Test reason");
    }

    private PostReport createTestPostReport(UUID postId, String reason) {
        PostReport report = new PostReport();
        report.setId(UUID.randomUUID());
        report.setPostId(postId);
        report.setReporterUserId(UUID.randomUUID());
        report.setReason(reason);
        report.setCreatedAt(OffsetDateTime.now());
        return report;
    }

    private PostReport createTestPostReportWithReporter(UUID postId, UUID reporterId) {
        PostReport report = new PostReport();
        report.setId(UUID.randomUUID());
        report.setPostId(postId);
        report.setReporterUserId(reporterId);
        report.setReason("Test reason");
        report.setCreatedAt(OffsetDateTime.now());
        return report;
    }

    private CommentReport createTestCommentReport(UUID commentId) {
        return createTestCommentReport(commentId, "Test reason");
    }

    private CommentReport createTestCommentReport(UUID commentId, String reason) {
        CommentReport report = new CommentReport();
        report.setId(UUID.randomUUID());
        report.setCommentId(commentId);
        report.setReporterUserId(UUID.randomUUID());
        report.setReason(reason);
        report.setCreatedAt(OffsetDateTime.now());
        return report;
    }

    private CommentReport createTestCommentReportWithReporter(UUID commentId, UUID reporterId) {
        CommentReport report = new CommentReport();
        report.setId(UUID.randomUUID());
        report.setCommentId(commentId);
        report.setReporterUserId(reporterId);
        report.setReason("Test reason");
        report.setCreatedAt(OffsetDateTime.now());
        return report;
    }
}
