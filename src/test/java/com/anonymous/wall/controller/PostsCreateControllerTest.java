package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.CreatePostRequestPostType;
import com.anonymous.wall.model.CreatePostRequestWall;
import com.anonymous.wall.model.PostDTO;
import com.anonymous.wall.repository.PollOptionRepository;
import com.anonymous.wall.repository.PollVoteRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Posts Controller - Create Post Tests")
class PostsCreateControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    PostRepository postRepository;

    @Inject
    PollOptionRepository pollOptionRepository;

    @Inject
    PollVoteRepository pollVoteRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/posts";

    private UserEntity testUserCampus;
    private UserEntity testUserDifferentSchool;
    private String jwtTokenCampus;
    private String jwtTokenDifferentSchool;

    @BeforeEach
    void setUp() {
        pollVoteRepository.deleteAll();
        pollOptionRepository.deleteAll();
        postRepository.deleteAll();

        testUserCampus = new UserEntity();
        testUserCampus.setEmail("student" + System.currentTimeMillis() + "@harvard.edu");
        testUserCampus.setSchoolDomain("harvard.edu");
        testUserCampus.setVerified(true);
        testUserCampus.setPasswordSet(true);
        testUserCampus = userRepository.save(testUserCampus);
        jwtTokenCampus = jwtTokenService.generateToken(testUserCampus);

        testUserDifferentSchool = new UserEntity();
        testUserDifferentSchool.setEmail("student" + System.currentTimeMillis() + "@mit.edu");
        testUserDifferentSchool.setSchoolDomain("mit.edu");
        testUserDifferentSchool.setVerified(true);
        testUserDifferentSchool.setPasswordSet(true);
        testUserDifferentSchool = userRepository.save(testUserDifferentSchool);
        jwtTokenDifferentSchool = jwtTokenService.generateToken(testUserDifferentSchool);
    }

    @AfterEach
    void tearDown() {
        pollVoteRepository.deleteAll();
        pollOptionRepository.deleteAll();
        postRepository.deleteAll();
    }

    private CreatePostRequest post(String title, String content, String wall) {
        CreatePostRequest request = new CreatePostRequest(title, content != null ? content : "");
        if (wall != null) {
            try {
                request.setWall(CreatePostRequestWall.fromValue(wall));
            } catch (IllegalArgumentException ignored) {
                // let the server reject it
            }
        }
        return request;
    }

    private CreatePostRequest post(String title, String content) {
        return post(title, content, null);
    }

    private CreatePostRequest poll(String title, String wall, String... options) {
        CreatePostRequest request = new CreatePostRequest(title, "");
        request.setPostType(CreatePostRequestPostType.POLL);
        if (wall != null) {
            try {
                request.setWall(CreatePostRequestWall.fromValue(wall));
            } catch (IllegalArgumentException ignored) {}
        }
        request.setPollOptions(List.of(options));
        return request;
    }

    @Nested
    @DisplayName("Create Post - Positive Cases")
    class CreatePostPositiveTests {

        @Test
        @DisplayName("Should create campus post with valid content")
        void shouldCreateCampusPost() {
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Campus Post", "This is a great campus post!", "campus"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            PostDTO body = response.body();
            assertNotNull(body);
            assertEquals("This is a great campus post!", body.getContent());
            assertEquals("campus", body.getWall().toString().toLowerCase());
            assertNotNull(body.getId());
            assertTrue(body.getAuthor().getIsAnonymous());
            assertEquals(0, body.getLikes());
            assertEquals(0, body.getComments());

            Optional<Post> savedPost = postRepository.findById(body.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("This is a great campus post!", savedPost.get().getContent());
            assertEquals("campus", savedPost.get().getWall());
            assertEquals("harvard.edu", savedPost.get().getSchoolDomain());
            assertEquals(testUserCampus.getId(), savedPost.get().getUserId());
        }

        @Test
        @DisplayName("Should create national post with valid content")
        void shouldCreateNationalPost() {
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test National Post", "This is a national post visible to everyone!", "national"))
                            .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            PostDTO body = response.body();
            assertNotNull(body);
            assertEquals("This is a national post visible to everyone!", body.getContent());
            assertEquals("national", body.getWall().toString().toLowerCase());

            Optional<Post> savedPost = postRepository.findById(body.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("national", savedPost.get().getWall());
        }

        @Test
        @DisplayName("Should default to campus wall when not specified")
        void shouldDefaultToCampusWall() {
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Default Campus Post", "Default campus post"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("campus", response.body().getWall().toString().toLowerCase());

            Optional<Post> savedPost = postRepository.findById(response.body().getId());
            assertTrue(savedPost.isPresent());
            assertEquals("campus", savedPost.get().getWall());
        }

        @Test
        @DisplayName("Should create post with special characters")
        void shouldCreatePostWithSpecialCharacters() {
            String content = "Check this out! @everyone #campus 💯 This is awesome🎉";
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Special Characters Post", content))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals(content, response.body().getContent());
        }

        @Test
        @DisplayName("Should create post with special characters in title")
        void shouldCreatePostWithSpecialCharactersInTitle() {
            String title = "🎉 Special Post @mention #topic";
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post(title, "Content with special title"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals(title, response.body().getTitle());
        }

        @Test
        @DisplayName("Should create post with maximum title length (255 characters)")
        void shouldCreatePostWithMaximumTitleLength() {
            String title = "T".repeat(255);
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post(title, "Content with max length title"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals(255, response.body().getTitle().length());
            assertEquals(title, response.body().getTitle());
        }

        @Test
        @DisplayName("Should create post with minimum title length (1 character)")
        void shouldCreatePostWithMinimumTitleLength() {
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("T", "Content with minimum title length"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("T", response.body().getTitle());
            assertEquals(1, response.body().getTitle().length());
        }

        @Test
        @DisplayName("Should include title in response")
        void shouldIncludeTitleInResponse() {
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Title Response", "Test content"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            PostDTO postDTO = response.body();
            assertNotNull(postDTO.getTitle());
            assertEquals("Test Title Response", postDTO.getTitle());
        }

        @Test
        @DisplayName("Should create multiple posts from same user")
        void shouldCreateMultiplePostsFromSameUser() {
            HttpResponse<PostDTO> response1 = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test First Post", "First post"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );
            HttpResponse<PostDTO> response2 = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Second Post", "Second post"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response1.getStatus());
            assertEquals(HttpStatus.CREATED, response2.getStatus());
            assertNotEquals(response1.body().getId(), response2.body().getId());
            assertEquals("First post", response1.body().getContent());
            assertEquals("Second post", response2.body().getContent());
        }
    }

    @Nested
    @DisplayName("Create Post - Negative Cases")
    class CreatePostNegativeTests {

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuthentication() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, post("Test Unauthorized Post", "Unauthorized post")),
                            PostDTO.class
                    )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail with empty content")
        void shouldFailWithEmptyContent() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, post("Test Empty Content", ""))
                                    .header("Authorization", "Bearer " + jwtTokenCampus),
                            PostDTO.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when title is missing")
        void shouldFailWithMissingTitle() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, post(null, "Content without title"))
                                    .header("Authorization", "Bearer " + jwtTokenCampus),
                            PostDTO.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail with empty title")
        void shouldFailWithEmptyTitle() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, post("", "Content with empty title"))
                                    .header("Authorization", "Bearer " + jwtTokenCampus),
                            PostDTO.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail with whitespace-only title")
        void shouldFailWithWhitespaceOnlyTitle() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, post("   \n\t   ", "Content with whitespace title"))
                                    .header("Authorization", "Bearer " + jwtTokenCampus),
                            PostDTO.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when title exceeds maximum length (255 characters)")
        void shouldFailWithTitleTooLong() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, post("T".repeat(256), "Content with title too long"))
                                    .header("Authorization", "Bearer " + jwtTokenCampus),
                            PostDTO.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when content exceeds maximum length")
        void shouldFailWithContentTooLong() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, post("Test Content Too Long", "X".repeat(5001)))
                                    .header("Authorization", "Bearer " + jwtTokenCampus),
                            PostDTO.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail with invalid wall type")
        void shouldFailWithInvalidWallType() {
            // Send raw map to bypass enum validation on client side
            Map<String, Object> rawRequest = Map.of(
                    "title", "Test Invalid Wall Type",
                    "content", "Post to invalid wall",
                    "wall", "invalid"
            );
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, rawRequest)
                                    .header("Authorization", "Bearer " + jwtTokenCampus),
                            PostDTO.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Create Post - Edge Cases")
    class CreatePostEdgeCaseTests {

        @Test
        @DisplayName("Should fail when user without school domain tries to post to campus")
        void shouldFailCampusPostWithoutSchoolDomain() {
            UserEntity userNoSchool = new UserEntity();
            userNoSchool.setEmail("noschool" + System.currentTimeMillis() + "@gmail.com");
            userNoSchool.setSchoolDomain(null);
            userNoSchool.setVerified(true);
            userNoSchool.setPasswordSet(true);
            userNoSchool = userRepository.save(userNoSchool);
            String tokenNoSchool = jwtTokenService.generateToken(userNoSchool);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, post("Test Campus Post Without School Domain", "Campus post without school domain", "campus"))
                                    .header("Authorization", "Bearer " + tokenNoSchool),
                            PostDTO.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should allow national post even without school domain")
        void shouldAllowNationalPostWithoutSchoolDomain() {
            UserEntity userNoSchool = new UserEntity();
            userNoSchool.setEmail("noschool" + System.currentTimeMillis() + "@gmail.com");
            userNoSchool.setSchoolDomain(null);
            userNoSchool.setVerified(true);
            userNoSchool.setPasswordSet(true);
            userNoSchool = userRepository.save(userNoSchool);
            String tokenNoSchool = jwtTokenService.generateToken(userNoSchool);

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test National Post Without School Domain", "National post from user without school", "national"))
                            .header("Authorization", "Bearer " + tokenNoSchool),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("national", response.body().getWall().toString().toLowerCase());
        }

        @Test
        @DisplayName("Should create post with maximum length (5000 characters)")
        void shouldCreatePostWithMaximumLength() {
            String content = "X".repeat(5000);
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Maximum Length", content))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals(5000, response.body().getContent().length());
        }

        @Test
        @DisplayName("Should create post with minimum length (1 character)")
        void shouldCreatePostWithMinimumLength() {
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Minimum Length", "A"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("A", response.body().getContent());
        }
    }

    @Nested
    @DisplayName("Create Post - Security Tests")
    class CreatePostSecurityTests {

        @Test
        @DisplayName("Should anonymize user in response")
        void shouldAnonymizeUser() {
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Anonymity", "Test anonymity"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            assertTrue(response.body().getAuthor().getIsAnonymous());
            assertNotNull(response.body().getAuthor().getId());
        }

        @Test
        @DisplayName("Post should be created with correct user ID in database")
        void shouldStoreCorrectUserIdInDatabase() {
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Correct User", "Verify correct user"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );

            Optional<Post> savedPost = postRepository.findById(response.body().getId());
            assertTrue(savedPost.isPresent());
            assertEquals(testUserCampus.getId(), savedPost.get().getUserId());
        }

        @Test
        @DisplayName("Different users should have separate posts")
        void shouldKeepUsersPostsSeparate() {
            HttpResponse<PostDTO> response1 = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test User 1 Post", "User 1 post"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    PostDTO.class
            );
            HttpResponse<PostDTO> response2 = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test User 2 Post", "User 2 post"))
                            .header("Authorization", "Bearer " + jwtTokenDifferentSchool),
                    PostDTO.class
            );

            Optional<Post> post1 = postRepository.findById(response1.body().getId());
            Optional<Post> post2 = postRepository.findById(response2.body().getId());
            assertTrue(post1.isPresent());
            assertTrue(post2.isPresent());
            assertEquals(testUserCampus.getId(), post1.get().getUserId());
            assertEquals(testUserDifferentSchool.getId(), post2.get().getUserId());
        }
    }

    @Nested
    @DisplayName("Create Post - Profile Name Tests")
    class CreatePostProfileNameTests {

        private UserEntity userWithDefaultName;
        private UserEntity userWithCustomName;
        private String tokenDefaultName;
        private String tokenCustomName;

        @BeforeEach
        void setUp() {
            userWithDefaultName = new UserEntity();
            userWithDefaultName.setEmail("default" + System.currentTimeMillis() + "@harvard.edu");
            userWithDefaultName.setSchoolDomain("harvard.edu");
            userWithDefaultName.setProfileName("Anonymous");
            userWithDefaultName.setVerified(true);
            userWithDefaultName.setPasswordSet(true);
            userWithDefaultName = userRepository.save(userWithDefaultName);
            tokenDefaultName = jwtTokenService.generateToken(userWithDefaultName);

            userWithCustomName = new UserEntity();
            userWithCustomName.setEmail("custom" + System.currentTimeMillis() + "@harvard.edu");
            userWithCustomName.setSchoolDomain("harvard.edu");
            userWithCustomName.setProfileName("John Doe");
            userWithCustomName.setVerified(true);
            userWithCustomName.setPasswordSet(true);
            userWithCustomName = userRepository.save(userWithCustomName);
            tokenCustomName = jwtTokenService.generateToken(userWithCustomName);
        }

        @Test
        @DisplayName("Should capture default profile name 'Anonymous' in post")
        void shouldCaptureDefaultProfileNameInPost() {
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Default Post", "Post with default profile name", "campus"))
                            .header("Authorization", "Bearer " + tokenDefaultName),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            PostDTO postDTO = response.body();
            assertNotNull(postDTO.getAuthor());
            assertEquals("Anonymous", postDTO.getAuthor().getProfileName());

            Optional<Post> savedPost = postRepository.findById(postDTO.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("Anonymous", savedPost.get().getProfileName());
        }

        @Test
        @DisplayName("Should capture custom profile name in post")
        void shouldCaptureCustomProfileNameInPost() {
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Custom Post", "Post with custom profile name", "campus"))
                            .header("Authorization", "Bearer " + tokenCustomName),
                    PostDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            PostDTO postDTO = response.body();
            assertNotNull(postDTO.getAuthor());
            assertEquals("John Doe", postDTO.getAuthor().getProfileName());

            Optional<Post> savedPost = postRepository.findById(postDTO.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("John Doe", savedPost.get().getProfileName());
        }

        @Test
        @DisplayName("Should preserve original profile name after user changes name")
        void shouldPreserveOriginalProfileNameAfterUserChanges() {
            userWithDefaultName.setProfileName("Custom Name");
            userRepository.update(userWithDefaultName);

            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Custom Name Post", "Post with custom name", "campus"))
                            .header("Authorization", "Bearer " + tokenDefaultName),
                    PostDTO.class
            );

            PostDTO postDTO = response.body();
            assertEquals("Custom Name", postDTO.getAuthor().getProfileName());

            userWithDefaultName.setProfileName("New Name");
            userRepository.update(userWithDefaultName);

            Optional<Post> savedPost = postRepository.findById(postDTO.getId());
            assertTrue(savedPost.isPresent());
            assertEquals("Custom Name", savedPost.get().getProfileName());

            HttpResponse<PostDTO> response2 = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test New Post", "New post", "campus"))
                            .header("Authorization", "Bearer " + tokenDefaultName),
                    PostDTO.class
            );

            assertEquals("New Name", response2.body().getAuthor().getProfileName());
        }

        @Test
        @DisplayName("Post author profile name should not equal user ID")
        void postAuthorProfileNameShouldNotEqualUserId() {
            HttpResponse<PostDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Test Post", "Post content", "campus"))
                            .header("Authorization", "Bearer " + tokenCustomName),
                    PostDTO.class
            );

            PostDTO postDTO = response.body();
            assertNotNull(postDTO.getAuthor());
            assertEquals("John Doe", postDTO.getAuthor().getProfileName());
            assertNotNull(postDTO.getAuthor().getId());
            assertNotEquals("John Doe", postDTO.getAuthor().getId());
        }
    }

    @Nested
    @DisplayName("Create Poll Post - Positive Cases")
    class CreatePollPostPositiveTests {

        @Test
        @DisplayName("Should create poll post with 2 options")
        void shouldCreatePollWith2Options() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, poll("Best language?", "campus", "Java", "Python"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            Map<?, ?> body = response.body();
            assertEquals("poll", body.get("postType"));
            assertEquals(0, body.get("totalVotes"));

            Map<?, ?> pollData = (Map<?, ?>) body.get("poll");
            assertNotNull(pollData);
            List<?> options = (List<?>) pollData.get("options");
            assertEquals(2, options.size());
        }

        @Test
        @DisplayName("Should create poll post with 4 options")
        void shouldCreatePollWith4Options() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, poll("Favorite season?", "campus", "Spring", "Summer", "Autumn", "Winter"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            Map<?, ?> pollData = (Map<?, ?>) response.body().get("poll");
            assertNotNull(pollData);
            assertEquals(4, ((List<?>) pollData.get("options")).size());
        }

        @Test
        @DisplayName("Poll post should have postType=poll in response")
        void shouldReturnCorrectPostTypeInResponse() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, poll("Poll?", "campus", "Yes", "No"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("poll", response.body().get("postType"));
        }

        @Test
        @DisplayName("Standard post should have postType=standard in response")
        void shouldReturnStandardPostType() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, post("Normal Post", "Content here"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            Object postType = response.body().get("postType");
            assertTrue(postType == null || "standard".equals(postType.toString().toLowerCase()));
        }

        @Test
        @DisplayName("Poll post content is optional")
        void shouldCreatePollWithoutContent() {
            CreatePostRequest request = new CreatePostRequest("Poll without content", "");
            request.setPostType(CreatePostRequestPostType.POLL);
            request.setWall(CreatePostRequestWall.CAMPUS);
            request.setPollOptions(List.of("Option A", "Option B"));

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request)
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("poll", response.body().get("postType"));
        }

        @Test
        @DisplayName("Poll options should preserve display order")
        void shouldPreserveOptionDisplayOrder() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, poll("Order test?", "campus", "First", "Second", "Third"))
                            .header("Authorization", "Bearer " + jwtTokenCampus),
                    Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            Map<?, ?> pollData = (Map<?, ?>) response.body().get("poll");
            List<?> options = (List<?>) pollData.get("options");
            assertEquals(3, options.size());
            assertEquals(0, ((Number) ((Map<?, ?>) options.get(0)).get("displayOrder")).intValue());
            assertEquals(1, ((Number) ((Map<?, ?>) options.get(1)).get("displayOrder")).intValue());
            assertEquals(2, ((Number) ((Map<?, ?>) options.get(2)).get("displayOrder")).intValue());
        }
    }

    @Nested
    @DisplayName("Create Poll Post - Negative Cases")
    class CreatePollPostNegativeTests {

        @Test
        @DisplayName("Should fail when poll has only 1 option")
        void shouldFailWith1PollOption() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, poll("Bad Poll", "campus", "Only option"))
                                    .header("Authorization", "Bearer " + jwtTokenCampus),
                            Map.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when poll has 5 options")
        void shouldFailWith5PollOptions() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, poll("Bad Poll", "campus", "A", "B", "C", "D", "E"))
                                    .header("Authorization", "Bearer " + jwtTokenCampus),
                            Map.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when a poll option text exceeds 100 characters")
        void shouldFailWithPollOptionOver100Chars() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, poll("Poll", "campus", "Short option", "X".repeat(101)))
                                    .header("Authorization", "Bearer " + jwtTokenCampus),
                            Map.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when poll has no options at all")
        void shouldFailWithNoPollOptions() {
            CreatePostRequest request = new CreatePostRequest("Poll no options", "");
            request.setPostType(CreatePostRequestPostType.POLL);
            request.setWall(CreatePostRequestWall.CAMPUS);
            request.setPollOptions(List.of());

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, request)
                                    .header("Authorization", "Bearer " + jwtTokenCampus),
                            Map.class
                    )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
    }
}