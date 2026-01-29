package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("PostsServiceImpl - Create Post Tests")
class PostsServiceImplCreatePostTest {

    @Inject
    private PostsService postsService;

    @Inject
    private PostRepository postRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUserHarvard;
    private UserEntity testUserMIT;

    @BeforeEach
    void setUp() {
        // Create Harvard user
        testUserHarvard = new UserEntity();
        testUserHarvard.setEmail("student" + System.currentTimeMillis() + "@harvard.edu");
        testUserHarvard.setSchoolDomain("harvard.edu");
        testUserHarvard.setVerified(true);
        testUserHarvard.setPasswordSet(true);
        testUserHarvard = userRepository.save(testUserHarvard);

        // Create MIT user
        testUserMIT = new UserEntity();
        testUserMIT.setEmail("student" + System.currentTimeMillis() + "@mit.edu");
        testUserMIT.setSchoolDomain("mit.edu");
        testUserMIT.setVerified(true);
        testUserMIT.setPasswordSet(true);
        testUserMIT = userRepository.save(testUserMIT);

        postRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create Post - Positive Cases")
    class CreatePostPositiveTests {

        @Test
        @DisplayName("Should create campus post with valid content")
        void shouldCreateCampusPost() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("Great campus post!");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertNotNull(result);
            assertEquals("Great campus post!", result.getContent());
            assertEquals("campus", result.getWall());
            assertEquals("harvard.edu", result.getSchoolDomain());
            assertEquals(testUserHarvard.getId(), result.getUserId());
        }

        @Test
        @DisplayName("Should create post with minimum content (1 character)")
        void shouldCreatePostWithMinimumLength() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("A");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(1, result.getContent().length());
        }

        @Test
        @DisplayName("Should create post with maximum content (5000 characters)")
        void shouldCreatePostWithMaximumLength() {
            // Arrange
            String maxContent = "X".repeat(5000);
            CreatePostRequest request = new CreatePostRequest(maxContent);

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(5000, result.getContent().length());
        }

        @Test
        @DisplayName("Should create post with special characters")
        void shouldCreatePostWithSpecialCharacters() {
            // Arrange
            String content = "Check this 🎉 @mention #hashtag";
            CreatePostRequest request = new CreatePostRequest(content);

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(content, result.getContent());
        }

        @Test
        @DisplayName("Should create post with newlines")
        void shouldCreatePostWithFormatting() {
            // Arrange
            String content = "Line 1\nLine 2\nLine 3";
            CreatePostRequest request = new CreatePostRequest(content);

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(content, result.getContent());
        }

        @Test
        @DisplayName("Should create multiple posts from same user")
        void shouldCreateMultiplePostsFromSameUser() {
            // Arrange
            CreatePostRequest request1 = new CreatePostRequest("First post");
            CreatePostRequest request2 = new CreatePostRequest("Second post");

            // Act
            Post result1 = postsService.createPost(request1, testUserHarvard.getId());
            Post result2 = postsService.createPost(request2, testUserHarvard.getId());

            // Assert
            assertNotEquals(result1.getId(), result2.getId());
            assertEquals("First post", result1.getContent());
            assertEquals("Second post", result2.getContent());
        }

        @Test
        @DisplayName("Should preserve user ID in post")
        void shouldPreserveUserIdInPost() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("User test");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(testUserHarvard.getId(), result.getUserId());
        }

        @Test
        @DisplayName("Should create posts from different users")
        void shouldCreatePostsFromDifferentUsers() {
            // Arrange
            CreatePostRequest request1 = new CreatePostRequest("Harvard post");
            CreatePostRequest request2 = new CreatePostRequest("MIT post");

            // Act
            Post result1 = postsService.createPost(request1, testUserHarvard.getId());
            Post result2 = postsService.createPost(request2, testUserMIT.getId());

            // Assert
            assertEquals("harvard.edu", result1.getSchoolDomain());
            assertEquals("mit.edu", result2.getSchoolDomain());
        }
    }

    @Nested
    @DisplayName("Create Post - Negative Cases")
    class CreatePostNegativeTests {

        @Test
        @DisplayName("Should fail with empty content")
        void shouldFailWithEmptyContent() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.createPost(request, testUserHarvard.getId())
            );
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should fail with whitespace-only content")
        void shouldFailWithWhitespaceOnlyContent() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("   \n\t   ");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.createPost(request, testUserHarvard.getId())
            );
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when content exceeds 5000 characters")
        void shouldFailWithContentTooLong() {
            // Arrange
            String tooLongContent = "X".repeat(5001);
            CreatePostRequest request = new CreatePostRequest(tooLongContent);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.createPost(request, testUserHarvard.getId())
            );
            assertTrue(exception.getMessage().contains("exceeds maximum length"));
        }

        @Test
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            // Arrange
            UUID nonexistentUserId = UUID.randomUUID();
            CreatePostRequest request = new CreatePostRequest("Content");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.createPost(request, nonexistentUserId)
            );
            assertTrue(exception.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("Should fail campus post when user has no school domain")
        void shouldFailCampusPostWithoutSchoolDomain() {
            // Arrange
            UserEntity userNoSchool = new UserEntity();
            userNoSchool.setEmail("noschool" + System.currentTimeMillis() + "@gmail.com");
            userNoSchool.setSchoolDomain(null);
            userNoSchool.setVerified(true);
            userNoSchool.setPasswordSet(true);
            userNoSchool = userRepository.save(userNoSchool);

            CreatePostRequest request = new CreatePostRequest("Campus post");
            UUID userId = userNoSchool.getId();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.createPost(request, userId)
            );
            assertTrue(exception.getMessage().contains("Cannot post to campus wall"));
        }
    }

    @Nested
    @DisplayName("Create Post - Business Logic")
    class CreatePostBusinessLogicTests {

        @Test
        @DisplayName("Should use user's school domain for campus post")
        void shouldUsersSchoolDomainForCampusPost() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("Campus content");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals("harvard.edu", result.getSchoolDomain());
        }

        @Test
        @DisplayName("Should initialize post with zero likes and comments")
        void shouldInitializePostWithZeroStats() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("Stats test");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(0, result.getLikeCount());
            assertEquals(0, result.getCommentCount());
            assertFalse(result.isLiked());
        }

        @Test
        @DisplayName("Should set timestamps when creating post")
        void shouldSetTimestampsOnCreation() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("Timestamp test");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
        }

        @Test
        @DisplayName("Different users should have different school domains")
        void shouldStoreDifferentSchoolDomains() {
            // Arrange
            CreatePostRequest harvardRequest = new CreatePostRequest("Harvard campus");
            CreatePostRequest mitRequest = new CreatePostRequest("MIT campus");

            // Act
            Post harvardPost = postsService.createPost(harvardRequest, testUserHarvard.getId());
            Post mitPost = postsService.createPost(mitRequest, testUserMIT.getId());

            // Assert
            assertEquals("harvard.edu", harvardPost.getSchoolDomain());
            assertEquals("mit.edu", mitPost.getSchoolDomain());
            assertNotEquals(harvardPost.getSchoolDomain(), mitPost.getSchoolDomain());
        }
    }

    @Nested
    @DisplayName("Create Post - Data Persistence")
    class CreatePostDataPersistenceTests {

        @Test
        @DisplayName("Post should be retrievable from database")
        void shouldPersistPostToDatabase() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("Database test");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            Optional<Post> retrieved = postRepository.findById(result.getId());
            assertTrue(retrieved.isPresent());
            assertEquals("Database test", retrieved.get().getContent());
            assertEquals("harvard.edu", retrieved.get().getSchoolDomain());
        }

        @Test
        @DisplayName("Multiple posts should be stored independently")
        void shouldStoreMultiplePostsIndependently() {
            // Arrange
            CreatePostRequest request1 = new CreatePostRequest("Post 1");
            CreatePostRequest request2 = new CreatePostRequest("Post 2");

            // Act
            Post result1 = postsService.createPost(request1, testUserHarvard.getId());
            Post result2 = postsService.createPost(request2, testUserHarvard.getId());

            // Assert
            Optional<Post> saved1 = postRepository.findById(result1.getId());
            Optional<Post> saved2 = postRepository.findById(result2.getId());
            assertTrue(saved1.isPresent());
            assertTrue(saved2.isPresent());
            assertNotEquals(saved1.get().getId(), saved2.get().getId());
        }

        @Test
        @DisplayName("Content should be accurately persisted")
        void shouldAccuratelyPersistContent() {
            // Arrange
            String content = "Line 1\nLine 2\nLine 3 with 🎉 emoji";
            CreatePostRequest request = new CreatePostRequest(content);

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            Optional<Post> saved = postRepository.findById(result.getId());
            assertTrue(saved.isPresent());
            assertEquals(content, saved.get().getContent());
        }
    }
}
