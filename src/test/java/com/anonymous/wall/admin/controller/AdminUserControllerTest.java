package com.anonymous.wall.admin.controller;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.AdminUserDTO;
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
@DisplayName("Admin User Controller Tests")
class AdminUserControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

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
        @DisplayName("Positive: Sort users by post count descending")
        void sortUsersByPostCountDesc() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=postCount&sortOrder=desc")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("data"));
        }

        @Test
        @DisplayName("Positive: Sort users by comment count descending")
        void sortUsersByCommentCountDesc() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=commentCount&sortOrder=desc")
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
}
