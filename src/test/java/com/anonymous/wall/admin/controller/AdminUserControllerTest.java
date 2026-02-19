package com.anonymous.wall.admin.controller;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.model.AdminUserDTO;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.CommentRepository;
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
@DisplayName("Admin User Controller Tests")
class AdminUserControllerTest {

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

    private static final String BASE_PATH = "/api/v1/admin/users";

    private UserEntity adminUser;
    private UserEntity moderatorUser;
    private UserEntity regularUser;
    private UserEntity targetUser;
    
    private String adminToken;
    private String moderatorToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // Create admin user
        adminUser = new UserEntity();
        adminUser.setEmail("admin" + System.currentTimeMillis() + "@test.edu");
        adminUser.setSchoolDomain("test.edu");
        adminUser.setVerified(true);
        adminUser.setPasswordSet(true);
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);
        adminToken = jwtTokenService.generateToken(adminUser);

        // Create moderator user
        moderatorUser = new UserEntity();
        moderatorUser.setEmail("moderator" + System.currentTimeMillis() + "@test.edu");
        moderatorUser.setSchoolDomain("test.edu");
        moderatorUser.setVerified(true);
        moderatorUser.setPasswordSet(true);
        moderatorUser.setRole("MODERATOR");
        moderatorUser = userRepository.save(moderatorUser);
        moderatorToken = jwtTokenService.generateToken(moderatorUser);

        // Create regular user
        regularUser = new UserEntity();
        regularUser.setEmail("user" + System.currentTimeMillis() + "@test.edu");
        regularUser.setSchoolDomain("test.edu");
        regularUser.setVerified(true);
        regularUser.setPasswordSet(true);
        regularUser.setRole("USER");
        regularUser = userRepository.save(regularUser);
        userToken = jwtTokenService.generateToken(regularUser);

        // Create target user for blocking/unblocking tests
        targetUser = new UserEntity();
        targetUser.setEmail("target" + System.currentTimeMillis() + "@test.edu");
        targetUser.setSchoolDomain("test.edu");
        targetUser.setVerified(true);
        targetUser.setPasswordSet(true);
        targetUser.setRole("USER");
        targetUser = userRepository.save(targetUser);
    }

    @Nested
    @DisplayName("List Users Endpoint Tests")
    class ListUsersTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can list all users")
        void adminCanListUsers() {
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
            
            List<Map> users = (List<Map>) response.body().get("data");
            assertTrue(users.size() >= 4); // At least our 4 test users
        }

        @Test
        @Order(2)
        @DisplayName("Positive: Moderator can list all users")
        void moderatorCanListUsers() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH)
                    .bearerAuth(moderatorToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
        }

        @Test
        @Order(3)
        @DisplayName("Negative: Regular user cannot list users")
        void regularUserCannotListUsers() {
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
        @Order(4)
        @DisplayName("Negative: Unauthenticated user cannot list users")
        void unauthenticatedUserCannotListUsers() {
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

        @Test
        @Order(5)
        @DisplayName("Positive: Pagination works correctly")
        void paginationWorksCorrectly() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?page=1&limit=2")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            Map pagination = (Map) response.body().get("pagination");
            assertEquals(1, pagination.get("page"));
            assertEquals(2, pagination.get("limit"));
        }
    }

    @Nested
    @DisplayName("Get User By ID Endpoint Tests")
    class GetUserByIdTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can get user details")
        void adminCanGetUserDetails() {
            // Act
            HttpResponse<AdminUserDTO> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + targetUser.getId())
                    .bearerAuth(adminToken),
                AdminUserDTO.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertEquals(targetUser.getId(), response.body().getId());
            assertEquals(targetUser.getEmail(), response.body().getEmail());
        }

        @Test
        @Order(2)
        @DisplayName("Negative: Regular user cannot get user details")
        void regularUserCannotGetUserDetails() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + targetUser.getId())
                        .bearerAuth(userToken),
                    AdminUserDTO.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Block User Endpoint Tests")
    class BlockUserTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can block a user")
        void adminCanBlockUser() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + targetUser.getId() + "/block", null)
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue(response.body().get("message").toString().contains("blocked"));

            // Verify in database
            UserEntity blockedUser = userRepository.findById(targetUser.getId()).orElseThrow();
            assertTrue(blockedUser.isBlocked());
        }

        @Test
        @Order(2)
        @DisplayName("Positive: Moderator can block a user")
        void moderatorCanBlockUser() {
            // Create another target user
            UserEntity anotherTarget = new UserEntity();
            anotherTarget.setEmail("blocktarget" + System.currentTimeMillis() + "@test.edu");
            anotherTarget.setSchoolDomain("test.edu");
            anotherTarget.setVerified(true);
            anotherTarget = userRepository.save(anotherTarget);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + anotherTarget.getId() + "/block", null)
                    .bearerAuth(moderatorToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());

            // Verify in database
            UserEntity blockedUser = userRepository.findById(anotherTarget.getId()).orElseThrow();
            assertTrue(blockedUser.isBlocked());
        }

        @Test
        @Order(3)
        @DisplayName("Negative: Regular user cannot block a user")
        void regularUserCannotBlockUser() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + UUID.randomUUID() + "/block", null)
                        .bearerAuth(userToken),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }

        @Test
        @Order(4)
        @DisplayName("Negative: Unauthenticated user cannot block a user")
        void unauthenticatedUserCannotBlockUser() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + UUID.randomUUID() + "/block", null),
                    Map.class
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Unblock User Endpoint Tests")
    class UnblockUserTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can unblock a user")
        void adminCanUnblockUser() {
            // First block the user
            targetUser.setBlocked(true);
            userRepository.update(targetUser);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + targetUser.getId() + "/unblock", null)
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue(response.body().get("message").toString().contains("unblocked"));

            // Verify in database
            UserEntity unblockedUser = userRepository.findById(targetUser.getId()).orElseThrow();
            assertFalse(unblockedUser.isBlocked());
        }

        @Test
        @Order(2)
        @DisplayName("Negative: Regular user cannot unblock a user")
        void regularUserCannotUnblockUser() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + UUID.randomUUID() + "/unblock", null)
                        .bearerAuth(userToken),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("User Sorting and Filtering Tests")
    class UserSortingAndFilteringTests {

        @Test
        @DisplayName("Positive: Sort users by creation time descending")
        void sortUsersByCreatedAtDesc() {
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
        @DisplayName("Positive: Sort users by creation time ascending")
        void sortUsersByCreatedAtAsc() {
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
        @DisplayName("Positive: Sort users by school domain")
        void sortUsersBySchoolDomain() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=schoolDomain")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Sort users by report count descending")
        void sortUsersByReportCountDesc() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=reportCount&sortOrder=desc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Sort users by report count ascending")
        void sortUsersByReportCountAsc() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=reportCount&sortOrder=asc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Filter users by blocked status - blocked only")
        void filterUsersByBlockedTrue() {
            // First block a user
            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + targetUser.getId() + "/block", null)
                    .bearerAuth(adminToken),
                Map.class
            );

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?blocked=true")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            
            List<Map> users = (List<Map>) response.body().get("data");
            // Verify we have at least one blocked user (since we just blocked one)
            assertFalse(users.isEmpty(), "Should have at least one blocked user");
            // Verify all returned users are blocked
            for (Map user : users) {
                assertTrue((Boolean) user.get("blocked"));
            }
        }

        @Test
        @DisplayName("Positive: Filter users by blocked status - active only")
        void filterUsersByBlockedFalse() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?blocked=false")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            
            List<Map> users = (List<Map>) response.body().get("data");
            // Verify we have at least one active user (our test users)
            assertFalse(users.isEmpty(), "Should have at least one active user");
            // Verify all returned users are not blocked
            for (Map user : users) {
                assertFalse((Boolean) user.get("blocked"));
            }
        }

        @Test
        @DisplayName("Positive: Combine filtering and sorting")
        void combineFilteringAndSorting() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?blocked=false&sortBy=createdAt&sortOrder=desc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }
    }

    @Nested
    @DisplayName("Get User Posts Endpoint Tests")
    class GetUserPostsTests {

        private Post testPost;

        @BeforeEach
        void setUpPosts() {
            // Create a test post for the target user
            testPost = new Post();
            testPost.setUserId(targetUser.getId());
            testPost.setProfileName(targetUser.getProfileName());
            testPost.setTitle("Test Post");
            testPost.setContent("Test post content");
            testPost.setWall("campus");
            testPost.setSchoolDomain(targetUser.getSchoolDomain());
            testPost.setLikeCount(0);
            testPost.setCommentCount(0);
            testPost.setHidden(false);
            testPost = postRepository.save(testPost);
        }

        @Test
        @DisplayName("Positive: Admin can get user's posts")
        void adminCanGetUserPosts() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + targetUser.getId() + "/posts")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            assertTrue(response.body().containsKey("pagination"));
            
            List<Map> posts = (List<Map>) response.body().get("data");
            assertNotNull(posts);
        }

        @Test
        @DisplayName("Positive: Moderator can get user's posts")
        void moderatorCanGetUserPosts() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + targetUser.getId() + "/posts")
                    .bearerAuth(moderatorToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Negative: Regular user cannot get user's posts")
        void regularUserCannotGetUserPosts() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + targetUser.getId() + "/posts")
                        .bearerAuth(userToken),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }

        @Test
        @DisplayName("Positive: Pagination works for user posts")
        void paginationWorksForUserPosts() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + targetUser.getId() + "/posts?page=1&limit=10")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> pagination = (Map<String, Object>) response.body().get("pagination");
            assertNotNull(pagination);
            assertEquals(1, pagination.get("page"));
            assertEquals(10, pagination.get("limit"));
        }

        @Test
        @DisplayName("Positive: Sorting works for user posts")
        void sortingWorksForUserPosts() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + targetUser.getId() + "/posts?sortBy=createdAt&sortOrder=desc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }
    }

    @Nested
    @DisplayName("Get User Comments Endpoint Tests")
    class GetUserCommentsTests {

        private Post testPost;
        private Comment testComment;

        @BeforeEach
        void setUpComments() {
            // Create a test post
            testPost = new Post();
            testPost.setUserId(adminUser.getId());
            testPost.setProfileName(adminUser.getProfileName());
            testPost.setTitle("Test Post for Comments");
            testPost.setContent("Test post content");
            testPost.setWall("campus");
            testPost.setSchoolDomain(adminUser.getSchoolDomain());
            testPost.setLikeCount(0);
            testPost.setCommentCount(0);
            testPost.setHidden(false);
            testPost = postRepository.save(testPost);

            // Create a test comment for the target user
            testComment = new Comment();
            testComment.setParentId(testPost.getId());
            testComment.setParentType("POST");
            testComment.setUserId(targetUser.getId());
            testComment.setProfileName(targetUser.getProfileName());
            testComment.setText("Test comment");
            testComment.setHidden(false);
            testComment = commentRepository.save(testComment);
        }

        @Test
        @DisplayName("Positive: Admin can get user's comments")
        void adminCanGetUserComments() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + targetUser.getId() + "/comments")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
            assertTrue(response.body().containsKey("pagination"));
            
            List<Map> comments = (List<Map>) response.body().get("data");
            assertNotNull(comments);
        }

        @Test
        @DisplayName("Positive: Moderator can get user's comments")
        void moderatorCanGetUserComments() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + targetUser.getId() + "/comments")
                    .bearerAuth(moderatorToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Negative: Regular user cannot get user's comments")
        void regularUserCannotGetUserComments() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + targetUser.getId() + "/comments")
                        .bearerAuth(userToken),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }

        @Test
        @DisplayName("Positive: Pagination works for user comments")
        void paginationWorksForUserComments() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + targetUser.getId() + "/comments?page=1&limit=10")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> pagination = (Map<String, Object>) response.body().get("pagination");
            assertNotNull(pagination);
            assertEquals(1, pagination.get("page"));
            assertEquals(10, pagination.get("limit"));
        }

        @Test
        @DisplayName("Positive: Sorting works for user comments")
        void sortingWorksForUserComments() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + targetUser.getId() + "/comments?sortBy=createdAt&sortOrder=desc")
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
