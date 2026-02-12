package com.anonymous.wall.repository;

import com.anonymous.wall.entity.PostReport;
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
@DisplayName("PostReportRepository Tests")
class PostReportRepositoryTest {

    @Inject
    PostReportRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    private PostReport createTestReport(UUID postId, UUID reporterUserId, String reason) {
        PostReport report = new PostReport();
        report.setPostId(postId);
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
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            PostReport report = createTestReport(postId, userId, "spam");

            // Act
            Optional<PostReport> found = repository.findById(report.getId());

            // Assert
            assertTrue(found.isPresent());
            assertEquals(postId, found.get().getPostId());
            assertEquals(userId, found.get().getReporterUserId());
        }
    }

    @Nested
    @DisplayName("ExistsByPostIdAndReporterUserId Tests")
    class ExistsByPostIdAndReporterUserIdTests {

        @Test
        @DisplayName("Positive: Should return true when report exists")
        void shouldReturnTrueWhenReportExists() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            createTestReport(postId, userId, "spam");

            // Act
            boolean exists = repository.existsByPostIdAndReporterUserId(postId, userId);

            // Assert
            assertTrue(exists);
        }

        @Test
        @DisplayName("Negative: Should return false when report doesn't exist")
        void shouldReturnFalseWhenReportDoesntExist() {
            // Act
            boolean exists = repository.existsByPostIdAndReporterUserId(UUID.randomUUID(), UUID.randomUUID());

            // Assert
            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("FindByPostIdAndReporterUserId Tests")
    class FindByPostIdAndReporterUserIdTests {

        @Test
        @DisplayName("Positive: Should find report by post and reporter")
        void shouldFindReportByPostAndReporter() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            createTestReport(postId, userId, "inappropriate");

            // Act
            Optional<PostReport> found = repository.findByPostIdAndReporterUserId(postId, userId);

            // Assert
            assertTrue(found.isPresent());
            assertEquals("inappropriate", found.get().getReason());
        }

        @Test
        @DisplayName("Negative: Should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            // Act
            Optional<PostReport> found = repository.findByPostIdAndReporterUserId(UUID.randomUUID(), UUID.randomUUID());

            // Assert
            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("CountByPostId Tests")
    class CountByPostIdTests {

        @Test
        @DisplayName("Positive: Should count reports for a post")
        void shouldCountReportsForPost() {
            // Arrange
            UUID postId = UUID.randomUUID();
            createTestReport(postId, UUID.randomUUID(), "spam");
            createTestReport(postId, UUID.randomUUID(), "inappropriate");
            createTestReport(postId, UUID.randomUUID(), "offensive");

            // Act
            long count = repository.countByPostId(postId);

            // Assert
            assertEquals(3, count);
        }

        @Test
        @DisplayName("Negative: Should return 0 for post with no reports")
        void shouldReturnZeroForPostWithNoReports() {
            // Act
            long count = repository.countByPostId(UUID.randomUUID());

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
            Page<PostReport> page = repository.findAll(pageable);

            // Assert
            assertNotNull(page);
            assertEquals(2, page.getContent().size());
        }
    }
}
