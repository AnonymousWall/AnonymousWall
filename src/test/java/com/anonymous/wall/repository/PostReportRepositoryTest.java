package com.anonymous.wall.repository;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.PostReport;
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
@DisplayName("PostReportRepository Tests")
class PostReportRepositoryTest {

    @Inject
    PostReportRepository repository;

    @Inject
    PostRepository postRepository;

    @Inject
    UserRepository userRepository;

    private Post testPost;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
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
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
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
            PostReport report = createTestReport(testPost.getId(), testUser.getId(), "spam");

            // Act
            Optional<PostReport> found = repository.findById(report.getId());

            // Assert
            assertTrue(found.isPresent());
            assertEquals(testPost.getId(), found.get().getPostId());
            assertEquals(testUser.getId(), found.get().getReporterUserId());
        }
    }

    @Nested
    @DisplayName("ExistsByPostIdAndReporterUserId Tests")
    class ExistsByPostIdAndReporterUserIdTests {

        @Test
        @DisplayName("Positive: Should return true when report exists")
        void shouldReturnTrueWhenReportExists() {
            // Arrange
            createTestReport(testPost.getId(), testUser.getId(), "spam");

            // Act
            boolean exists = repository.existsByPostIdAndReporterUserId(testPost.getId(), testUser.getId());

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
            boolean exists = repository.existsByPostIdAndReporterUserId(testPost.getId(), anotherUser.getId());

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
            createTestReport(testPost.getId(), testUser.getId(), "inappropriate");

            // Act
            Optional<PostReport> found = repository.findByPostIdAndReporterUserId(testPost.getId(), testUser.getId());

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
            Optional<PostReport> found = repository.findByPostIdAndReporterUserId(testPost.getId(), anotherUser.getId());

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

            createTestReport(testPost.getId(), testUser.getId(), "spam");
            createTestReport(testPost.getId(), user2.getId(), "inappropriate");
            createTestReport(testPost.getId(), user3.getId(), "offensive");

            // Act
            long count = repository.countByPostId(testPost.getId());

            // Assert
            assertEquals(3, count);
        }

        @Test
        @DisplayName("Negative: Should return 0 for post with no reports")
        void shouldReturnZeroForPostWithNoReports() {
            // Arrange
            Post postNoReports = new Post();
            postNoReports.setUserId(testUser.getId());
            postNoReports.setWall("national");
            postNoReports.setContent("Post with no reports");
            postNoReports.setCreatedAt(OffsetDateTime.now());
            postNoReports = postRepository.save(postNoReports);

            // Act
            long count = repository.countByPostId(postNoReports.getId());

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

            createTestReport(testPost.getId(), testUser.getId(), "spam");
            createTestReport(testPost.getId(), user2.getId(), "inappropriate");
            Pageable pageable = Pageable.from(0, 10);

            // Act
            Page<PostReport> page = repository.findAll(pageable);

            // Assert
            assertNotNull(page);
            assertEquals(2, page.getContent().size());
        }
    }
}
