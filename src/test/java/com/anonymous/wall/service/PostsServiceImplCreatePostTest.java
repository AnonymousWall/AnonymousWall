package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.PostsService;
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

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private PostLikeRepository postLikeRepository;

    private UserEntity testUserHarvard;
    private UserEntity testUserMIT;

    @BeforeEach
    void setUp() {
        // Clean up any leftover data
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();

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
        // Must delete in order: likes, comments, then posts
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create Post - Positive Cases")
    class CreatePostPositiveTests {

        @Test
        @DisplayName("Should create campus post with valid content")
        void shouldCreateCampusPost() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("Campus Post", "Great campus post!");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertNotNull(result);
            assertEquals("Campus Post", result.getTitle());
            assertEquals("Great campus post!", result.getContent());
            assertEquals("campus", result.getWall());
            assertEquals("harvard.edu", result.getSchoolDomain());
            assertEquals(testUserHarvard.getId(), result.getUserId());
        }

        @Test
        @DisplayName("Should create post with minimum content (1 character)")
        void shouldCreatePostWithMinimumLength() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("Title", "A");

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
            CreatePostRequest request = new CreatePostRequest("Max Content Title", maxContent);

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
            CreatePostRequest request = new CreatePostRequest("Special Post", content);

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
            CreatePostRequest request = new CreatePostRequest("Multi-line Post", content);

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(content, result.getContent());
        }

        @Test
        @DisplayName("Should create multiple posts from same user")
        void shouldCreateMultiplePostsFromSameUser() {
            // Arrange
            CreatePostRequest request1 = new CreatePostRequest("First Title", "First post");
            CreatePostRequest request2 = new CreatePostRequest("Second Title", "Second post");

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
            CreatePostRequest request = new CreatePostRequest("User Test", "User test");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(testUserHarvard.getId(), result.getUserId());
        }

        @Test
        @DisplayName("Should create posts from different users")
        void shouldCreatePostsFromDifferentUsers() {
            // Arrange
            CreatePostRequest request1 = new CreatePostRequest("Harvard Post", "Harvard post");
            CreatePostRequest request2 = new CreatePostRequest("MIT Post", "MIT post");

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
            CreatePostRequest request = new CreatePostRequest("Valid Title", "");

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
            CreatePostRequest request = new CreatePostRequest("Valid Title", "   \n\t   ");

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
            CreatePostRequest request = new CreatePostRequest("Valid Title", tooLongContent);

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
            CreatePostRequest request = new CreatePostRequest("Valid Title", "Content");

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

            CreatePostRequest request = new CreatePostRequest("Valid Title", "Campus post");
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
            CreatePostRequest request = new CreatePostRequest("Campus Title", "Campus content");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals("harvard.edu", result.getSchoolDomain());
        }

        @Test
        @DisplayName("Should initialize post with zero likes and comments")
        void shouldInitializePostWithZeroStats() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("Stats Title", "Stats test");

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
            CreatePostRequest request = new CreatePostRequest("Timestamp Title", "Timestamp test");

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
            CreatePostRequest harvardRequest = new CreatePostRequest("Harvard Title", "Harvard campus");
            CreatePostRequest mitRequest = new CreatePostRequest("MIT Title", "MIT campus");

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
            CreatePostRequest request = new CreatePostRequest("Database Title", "Database test");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            Optional<Post> retrieved = postRepository.findById(result.getId());
            assertTrue(retrieved.isPresent());
            assertEquals("Database Title", retrieved.get().getTitle());
            assertEquals("Database test", retrieved.get().getContent());
            assertEquals("harvard.edu", retrieved.get().getSchoolDomain());
        }

        @Test
        @DisplayName("Multiple posts should be stored independently")
        void shouldStoreMultiplePostsIndependently() {
            // Arrange
            CreatePostRequest request1 = new CreatePostRequest("Post 1 Title", "Post 1");
            CreatePostRequest request2 = new CreatePostRequest("Post 2 Title", "Post 2");

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
            CreatePostRequest request = new CreatePostRequest("Content Test Title", content);

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            Optional<Post> saved = postRepository.findById(result.getId());
            assertTrue(saved.isPresent());
            assertEquals(content, saved.get().getContent());
        }
    }

    @Nested
    @DisplayName("Create Post - Title Validation (NEW FEATURE)")
    class CreatePostTitleValidationTests {

        @Test
        @DisplayName("Should create post with valid title")
        void shouldCreatePostWithValidTitle() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("My Post Title", "Post content");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertNotNull(result);
            assertEquals("My Post Title", result.getTitle());
            assertEquals("Post content", result.getContent());
        }

        @Test
        @DisplayName("Should create post with minimum title (1 character)")
        void shouldCreatePostWithMinimumTitle() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("A", "Content here");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals("A", result.getTitle());
            assertEquals(1, result.getTitle().length());
        }

        @Test
        @DisplayName("Should create post with maximum title (255 characters)")
        void shouldCreatePostWithMaximumTitle() {
            // Arrange
            String maxTitle = "T".repeat(255);
            CreatePostRequest request = new CreatePostRequest(maxTitle, "Content");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(255, result.getTitle().length());
            assertEquals(maxTitle, result.getTitle());
        }

        @Test
        @DisplayName("Should create post with special characters in title")
        void shouldCreatePostWithSpecialCharactersInTitle() {
            // Arrange
            String title = "Check this 🎉 @mention #hashtag";
            CreatePostRequest request = new CreatePostRequest(title, "Content");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(title, result.getTitle());
        }

        @Test
        @DisplayName("Should create post with newlines in title")
        void shouldCreatePostWithNewlinesInTitle() {
            // Arrange
            String title = "Title Line 1\nTitle Line 2";
            CreatePostRequest request = new CreatePostRequest(title, "Content");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(title, result.getTitle());
        }

        @Test
        @DisplayName("Should fail when title is null")
        void shouldFailWhenTitleIsNull() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest(null, "Content");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.createPost(request, testUserHarvard.getId())
            );
            assertTrue(exception.getMessage().contains("title cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when title is empty string")
        void shouldFailWhenTitleIsEmpty() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("", "Content");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.createPost(request, testUserHarvard.getId())
            );
            assertTrue(exception.getMessage().contains("title cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when title is whitespace only")
        void shouldFailWhenTitleIsWhitespaceOnly() {
            // Arrange
            CreatePostRequest request = new CreatePostRequest("   \n\t   ", "Content");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.createPost(request, testUserHarvard.getId())
            );
            assertTrue(exception.getMessage().contains("title cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when title exceeds 255 characters")
        void shouldFailWhenTitleTooLong() {
            // Arrange
            String tooLongTitle = "T".repeat(256);
            CreatePostRequest request = new CreatePostRequest(tooLongTitle, "Content");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postsService.createPost(request, testUserHarvard.getId())
            );
            assertTrue(exception.getMessage().contains("exceeds maximum length of 255 characters"));
        }

        @Test
        @DisplayName("Should preserve title in database")
        void shouldPersistTitleToDatabase() {
            // Arrange
            String title = "Database Test Title";
            CreatePostRequest request = new CreatePostRequest(title, "Content");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            Optional<Post> saved = postRepository.findById(result.getId());
            assertTrue(saved.isPresent());
            assertEquals(title, saved.get().getTitle());
        }

        @Test
        @DisplayName("Multiple posts should have independent titles")
        void shouldStoreMultipleTitlesIndependently() {
            // Arrange
            CreatePostRequest request1 = new CreatePostRequest("Title 1", "Content 1");
            CreatePostRequest request2 = new CreatePostRequest("Title 2", "Content 2");

            // Act
            Post result1 = postsService.createPost(request1, testUserHarvard.getId());
            Post result2 = postsService.createPost(request2, testUserHarvard.getId());

            // Assert
            Optional<Post> saved1 = postRepository.findById(result1.getId());
            Optional<Post> saved2 = postRepository.findById(result2.getId());
            assertTrue(saved1.isPresent());
            assertTrue(saved2.isPresent());
            assertEquals("Title 1", saved1.get().getTitle());
            assertEquals("Title 2", saved2.get().getTitle());
            assertNotEquals(saved1.get().getTitle(), saved2.get().getTitle());
        }

        @Test
        @DisplayName("Title and content should be stored independently")
        void shouldStoreTitleAndContentIndependently() {
            // Arrange
            String title = "My Title";
            String content = "My Content";
            CreatePostRequest request = new CreatePostRequest(title, content);

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(title, result.getTitle());
            assertEquals(content, result.getContent());
            assertNotEquals(result.getTitle(), result.getContent());
        }

        @Test
        @DisplayName("Title with same content as another post should work")
        void shouldAllowSameTitleForDifferentPosts() {
            // Arrange
            CreatePostRequest request1 = new CreatePostRequest("Same Title", "Content 1");
            CreatePostRequest request2 = new CreatePostRequest("Same Title", "Content 2");

            // Act
            Post result1 = postsService.createPost(request1, testUserHarvard.getId());
            Post result2 = postsService.createPost(request2, testUserMIT.getId());

            // Assert
            assertEquals(result1.getTitle(), result2.getTitle());
            assertNotEquals(result1.getContent(), result2.getContent());
            assertNotEquals(result1.getId(), result2.getId());
        }

        @Test
        @DisplayName("Very long title with special characters should be accepted")
        void shouldAcceptVeryLongTitleWithSpecialCharacters() {
            // Arrange
            String longTitle = "This is a very long title with numbers 123456 and special chars !@#$%^&*() - exactly 255 chars".
                    concat(" ".repeat(255 - "This is a very long title with numbers 123456 and special chars !@#$%^&*() - exactly 255 chars".length()));
            CreatePostRequest request = new CreatePostRequest(longTitle.substring(0, 255), "Content");

            // Act
            Post result = postsService.createPost(request, testUserHarvard.getId());

            // Assert
            assertEquals(255, result.getTitle().length());
        }
    }
}
