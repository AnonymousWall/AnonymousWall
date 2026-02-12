package com.anonymous.wall.repository;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.CommentReport;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
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

    @Inject
    CommentRepository commentRepository;

    @Inject
    PostRepository postRepository;

    @Inject
    UserRepository userRepository;

    private Comment testComment;
    private UserEntity testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new UserEntity();
        testUser.setEmail("reporter" + System.currentTimeMillis() + "@test.edu");
        testUser.setSchoolDomain("test.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);

        // Create test post
        testPost = new Post();
        testPost.setUserId(testUser.getId());
        testPost.setWall("national");
        testPost.setContent("Test post");
        testPost.setCreatedAt(OffsetDateTime.now());
        testPost = postRepository.save(testPost);

        // Create test comment
        testComment = new Comment(testPost.getId(), testUser.getId(), "Test comment");
        testComment.setProfileName(testUser.getProfileName());
        testComment.setCreatedAt(OffsetDateTime.now());
        testComment = commentRepository.save(testComment);
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
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
            CommentReport report = createTestReport(testComment.getId(), testUser.getId(), "spam");

            // Act
            Optional<CommentReport> found = repository.findById(report.getId());

            // Assert
            assertTrue(found.isPresent());
            assertEquals(testComment.getId(), found.get().getCommentId());
            assertEquals(testUser.getId(), found.get().getReporterUserId());
        }
    }

    @Nested
    @DisplayName("ExistsByCommentIdAndReporterUserId Tests")
    class ExistsByCommentIdAndReporterUserIdTests {

        @Test
        @DisplayName("Positive: Should return true when report exists")
        void shouldReturnTrueWhenReportExists() {
            // Arrange
            createTestReport(testComment.getId(), testUser.getId(), "spam");

            // Act
            boolean exists = repository.existsByCommentIdAndReporterUserId(testComment.getId(), testUser.getId());

            // Assert
            assertTrue(exists);
        }

        @Test
        @DisplayName("Negative: Should return false when report doesn't exist")
        void shouldReturnFalseWhenReportDoesntExist() {
            // Arrange
            UserEntity anotherUser = new UserEntity();
            anotherUser.setEmail("another" + System.currentTimeMillis() + "@test.edu");
            anotherUser.setSchoolDomain("test.edu");
            anotherUser.setVerified(true);
            anotherUser = userRepository.save(anotherUser);

            // Act
            boolean exists = repository.existsByCommentIdAndReporterUserId(testComment.getId(), anotherUser.getId());

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
            createTestReport(testComment.getId(), testUser.getId(), "inappropriate");

            // Act
            Optional<CommentReport> found = repository.findByCommentIdAndReporterUserId(testComment.getId(), testUser.getId());

            // Assert
            assertTrue(found.isPresent());
            assertEquals("inappropriate", found.get().getReason());
        }

        @Test
        @DisplayName("Negative: Should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            // Arrange
            UserEntity anotherUser = new UserEntity();
            anotherUser.setEmail("notfound" + System.currentTimeMillis() + "@test.edu");
            anotherUser.setSchoolDomain("test.edu");
            anotherUser.setVerified(true);
            anotherUser = userRepository.save(anotherUser);

            // Act
            Optional<CommentReport> found = repository.findByCommentIdAndReporterUserId(testComment.getId(), anotherUser.getId());

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
            UserEntity user2 = new UserEntity();
            user2.setEmail("user2" + System.currentTimeMillis() + "@test.edu");
            user2.setSchoolDomain("test.edu");
            user2.setVerified(true);
            user2 = userRepository.save(user2);

            UserEntity user3 = new UserEntity();
            user3.setEmail("user3" + System.currentTimeMillis() + "@test.edu");
            user3.setSchoolDomain("test.edu");
            user3.setVerified(true);
            user3 = userRepository.save(user3);

            createTestReport(testComment.getId(), testUser.getId(), "spam");
            createTestReport(testComment.getId(), user2.getId(), "inappropriate");
            createTestReport(testComment.getId(), user3.getId(), "offensive");

            // Act
            long count = repository.countByCommentId(testComment.getId());

            // Assert
            assertEquals(3, count);
        }

        @Test
        @DisplayName("Negative: Should return 0 for comment with no reports")
        void shouldReturnZeroForCommentWithNoReports() {
            // Arrange
            Comment commentNoReports = new Comment(testPost.getId(), testUser.getId(), "No reports comment");
            commentNoReports.setProfileName(testUser.getProfileName());
            commentNoReports.setCreatedAt(OffsetDateTime.now());
            commentNoReports = commentRepository.save(commentNoReports);

            // Act
            long count = repository.countByCommentId(commentNoReports.getId());

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
            UserEntity user2 = new UserEntity();
            user2.setEmail("paguser" + System.currentTimeMillis() + "@test.edu");
            user2.setSchoolDomain("test.edu");
            user2.setVerified(true);
            user2 = userRepository.save(user2);

            createTestReport(testComment.getId(), testUser.getId(), "spam");
            createTestReport(testComment.getId(), user2.getId(), "inappropriate");
            Pageable pageable = Pageable.from(0, 10);

            // Act
            Page<CommentReport> page = repository.findAll(pageable);

            // Assert
            assertNotNull(page);
            assertEquals(2, page.getContent().size());
        }
    }
}
