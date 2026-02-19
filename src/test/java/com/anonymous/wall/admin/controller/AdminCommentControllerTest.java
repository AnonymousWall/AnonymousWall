package com.anonymous.wall.admin.controller;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.CommentRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Admin Comment Controller Tests")
class AdminCommentControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    PostRepository postRepository;

    @Inject
    CommentRepository commentRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/admin/comments";

    private UserEntity adminUser;
    private UserEntity regularUser;
    private Post testPost;
    private Comment testComment;
    
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // Clean up
        commentRepository.deleteAll();
        postRepository.deleteAll();

        // Create admin user
        adminUser = new UserEntity();
        adminUser.setEmail("admin" + System.currentTimeMillis() + "@test.edu");
        adminUser.setSchoolDomain("test.edu");
        adminUser.setVerified(true);
        adminUser.setPasswordSet(true);
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);
        adminToken = jwtTokenService.generateToken(adminUser);

        // Create regular user
        regularUser = new UserEntity();
        regularUser.setEmail("user" + System.currentTimeMillis() + "@test.edu");
        regularUser.setSchoolDomain("test.edu");
        regularUser.setVerified(true);
        regularUser.setPasswordSet(true);
        regularUser.setRole("USER");
        regularUser = userRepository.save(regularUser);
        userToken = jwtTokenService.generateToken(regularUser);

        // Create test post
        testPost = new Post();
        testPost.setUserId(regularUser.getId());
        testPost.setTitle("Test Post");
        testPost.setContent("Test content");
        testPost.setWall("campus");
        testPost.setSchoolDomain("test.edu");
        testPost = postRepository.save(testPost);

        // Create test comment
        testComment = new Comment();
        testComment.setParentId(testPost.getId());
        testComment.setParentType("POST");
        testComment.setUserId(regularUser.getId());
        testComment.setText("Test comment");
        testComment.setHidden(false);
        testComment = commentRepository.save(testComment);
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
    }

    @Nested
    @DisplayName("List Comments Endpoint Tests")
    class ListCommentsTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can list all comments")
        void adminCanListComments() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH)
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            assertTrue(response.body().containsKey("pagination"));
            
            List<Map> comments = (List<Map>) response.body().get("data");
            assertTrue(comments.size() >= 1); // At least our test comment
        }

        @Test
        @Order(2)
        @DisplayName("Negative: Regular user cannot list admin comments")
        void regularUserCannotListAdminComments() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH)
                        .bearerAuth(userToken),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }

        @Test
        @Order(3)
        @DisplayName("Negative: Unauthenticated user cannot list admin comments")
        void unauthenticatedUserCannotListAdminComments() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Delete Comment Endpoint Tests")
    class DeleteCommentTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can get a comment by ID")
        void adminCanGetCommentById() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testComment.getId())
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertEquals(testComment.getId().toString(), response.body().get("id"));
            assertEquals(testComment.getText(), response.body().get("text"));
            assertEquals(testComment.getParentId().toString(), response.body().get("postId"));
        }

        @Test
        @Order(2)
        @DisplayName("Negative: Regular user cannot get comment by ID via admin endpoint")
        void regularUserCannotGetCommentById() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + testComment.getId())
                        .bearerAuth(userToken),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }

        @Test
        @Order(3)
        @DisplayName("Negative: Unauthenticated user cannot get comment by ID")
        void unauthenticatedUserCannotGetCommentById() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + testComment.getId()),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @Order(4)
        @DisplayName("Negative: Admin gets error for non-existent comment when getting by ID")
        void adminGetsErrorForNonExistentCommentOnGet() {
            // Act & Assert
            UUID randomId = UUID.randomUUID();
            assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + randomId)
                        .bearerAuth(adminToken),
                    Map.class
                )
            );
        }

        @Test
        @Order(5)
        @DisplayName("Positive: Admin can soft-delete a comment")
        void adminCanSoftDeleteComment() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.DELETE(BASE_PATH + "/" + testComment.getId())
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue(response.body().get("message").toString().contains("deleted"));

            // Verify in database - comment should be hidden
            Comment deletedComment = commentRepository.findById(testComment.getId()).orElseThrow();
            assertTrue(deletedComment.isHidden());
        }

        @Test
        @Order(2)
        @DisplayName("Negative: Regular user cannot delete a comment via admin endpoint")
        void regularUserCannotDeleteComment() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.DELETE(BASE_PATH + "/" + testComment.getId())
                        .bearerAuth(userToken),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

            // Verify comment is still not hidden
            Comment comment = commentRepository.findById(testComment.getId()).orElseThrow();
            assertFalse(comment.isHidden());
        }

        @Test
        @Order(3)
        @DisplayName("Negative: Unauthenticated user cannot delete a comment")
        void unauthenticatedUserCannotDeleteComment() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.DELETE(BASE_PATH + "/" + testComment.getId()),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Comment Sorting and Filtering Tests")
    class CommentSortingAndFilteringTests {

        @Test
        @DisplayName("Positive: Sort comments by creation time descending")
        void sortCommentsByCreatedAtDesc() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=createdAt&sortOrder=desc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Sort comments by creation time ascending")
        void sortCommentsByCreatedAtAsc() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=createdAt&sortOrder=asc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Sort comments by author (userId)")
        void sortCommentsByAuthor() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=userId")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Filter comments by author userId")
        void filterCommentsByUserId() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?userId=" + regularUser.getId())
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Filter comments by hidden status")
        void filterCommentsByHidden() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?hidden=false")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Combine filtering and sorting")
        void combineFilteringAndSorting() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?hidden=false&sortBy=createdAt&sortOrder=desc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }
    }
}
