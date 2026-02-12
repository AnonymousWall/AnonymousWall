package com.anonymous.wall.repository;

import com.anonymous.wall.entity.CommentReport;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("CommentReportRepository Tests")
class CommentReportRepositoryTest {

    @Inject
    CommentReportRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    private CommentReport createTestReport(UUID commentId, UUID reporterUserId, String reason) {
        CommentReport report = new CommentReport();
        report.setCommentId(commentId);
        report.setReporterUserId(reporterUserId);
        report.setReason(reason);
        report.setCreatedAt(OffsetDateTime.now());
        return repository.save(report);
    }

    @Nested
    @DisplayName("Save and Find Tests")
    class SaveAndFindTests {

        @Test
        @DisplayName("Positive: Should save and find report")
        void shouldSaveAndFindReport() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            CommentReport report = createTestReport(commentId, userId, "spam");

            // Act
            Optional<CommentReport> found = repository.findById(report.getId());

            // Assert
            assertTrue(found.isPresent());
            assertEquals(commentId, found.get().getCommentId());
            assertEquals(userId, found.get().getReporterUserId());
        }
    }

    @Nested
    @DisplayName("ExistsByCommentIdAndReporterUserId Tests")
    class ExistsByCommentIdAndReporterUserIdTests {

        @Test
        @DisplayName("Positive: Should return true when report exists")
        void shouldReturnTrueWhenReportExists() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            createTestReport(commentId, userId, "spam");

            // Act
            boolean exists = repository.existsByCommentIdAndReporterUserId(commentId, userId);

            // Assert
            assertTrue(exists);
        }

        @Test
        @DisplayName("Negative: Should return false when report doesn't exist")
        void shouldReturnFalseWhenReportDoesntExist() {
            // Act
            boolean exists = repository.existsByCommentIdAndReporterUserId(UUID.randomUUID(), UUID.randomUUID());

            // Assert
            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("FindByCommentIdAndReporterUserId Tests")
    class FindByCommentIdAndReporterUserIdTests {

        @Test
        @DisplayName("Positive: Should find report by comment and reporter")
        void shouldFindReportByCommentAndReporter() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            createTestReport(commentId, userId, "inappropriate");

            // Act
            Optional<CommentReport> found = repository.findByCommentIdAndReporterUserId(commentId, userId);

            // Assert
            assertTrue(found.isPresent());
            assertEquals("inappropriate", found.get().getReason());
        }

        @Test
        @DisplayName("Negative: Should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            // Act
            Optional<CommentReport> found = repository.findByCommentIdAndReporterUserId(UUID.randomUUID(), UUID.randomUUID());

            // Assert
            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("CountByCommentId Tests")
    class CountByCommentIdTests {

        @Test
        @DisplayName("Positive: Should count reports for a comment")
        void shouldCountReportsForComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            createTestReport(commentId, UUID.randomUUID(), "spam");
            createTestReport(commentId, UUID.randomUUID(), "inappropriate");
            createTestReport(commentId, UUID.randomUUID(), "offensive");

            // Act
            long count = repository.countByCommentId(commentId);

            // Assert
            assertEquals(3, count);
        }

        @Test
        @DisplayName("Negative: Should return 0 for comment with no reports")
        void shouldReturnZeroForCommentWithNoReports() {
            // Act
            long count = repository.countByCommentId(UUID.randomUUID());

            // Assert
            assertEquals(0, count);
        }
    }

    @Nested
    @DisplayName("FindAll with Pagination Tests")
    class FindAllWithPaginationTests {

        @Test
        @DisplayName("Positive: Should return paginated reports")
        void shouldReturnPaginatedReports() {
            // Arrange
            createTestReport(UUID.randomUUID(), UUID.randomUUID(), "spam");
            createTestReport(UUID.randomUUID(), UUID.randomUUID(), "inappropriate");
            Pageable pageable = Pageable.from(0, 10);

            // Act
            Page<CommentReport> page = repository.findAll(pageable);

            // Assert
            assertNotNull(page);
            assertEquals(2, page.getContent().size());
        }
    }
}
