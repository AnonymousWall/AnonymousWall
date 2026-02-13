package com.anonymous.wall.admin.controller;

import com.anonymous.wall.entity.*;
import com.anonymous.wall.repository.*;
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

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Admin Report Controller Tests")
class AdminReportControllerTest {

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
    PostReportRepository postReportRepository;

    @Inject
    CommentReportRepository commentReportRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/admin/reports";

    private UserEntity adminUser;
    private UserEntity regularUser;
    private UserEntity reporterUser;
    private Post testPost;
    private Comment testComment;
    
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // Clean up
        postReportRepository.deleteAll();
        commentReportRepository.deleteAll();
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

        // Create reporter user
        reporterUser = new UserEntity();
        reporterUser.setEmail("reporter" + System.currentTimeMillis() + "@test.edu");
        reporterUser.setSchoolDomain("test.edu");
        reporterUser.setVerified(true);
        reporterUser = userRepository.save(reporterUser);

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
        testComment.setPostId(testPost.getId());
        testComment.setUserId(regularUser.getId());
        testComment.setText("Test comment");
        testComment = commentRepository.save(testComment);

        // Create post report
        PostReport postReport = new PostReport(testPost.getId(), reporterUser.getId(), regularUser.getId(), "Inappropriate content");
        postReportRepository.save(postReport);

        // Create comment report
        CommentReport commentReport = new CommentReport(testComment.getId(), reporterUser.getId(), regularUser.getId(), "Spam");
        commentReportRepository.save(commentReport);
    }

    @AfterEach
    void tearDown() {
        postReportRepository.deleteAll();
        commentReportRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
    }

    @Nested
    @DisplayName("List All Reports Endpoint Tests")
    class ListAllReportsTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can list all reports")
        void adminCanListAllReports() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH)
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("postReports"));
            assertTrue(response.body().containsKey("commentReports"));
            assertTrue(response.body().containsKey("pagination"));
            
            List<Map> postReports = (List<Map>) response.body().get("postReports");
            List<Map> commentReports = (List<Map>) response.body().get("commentReports");
            assertTrue(postReports.size() >= 1);
            assertTrue(commentReports.size() >= 1);
        }

        @Test
        @Order(2)
        @DisplayName("Positive: Admin can filter post reports only")
        void adminCanFilterPostReportsOnly() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?type=post")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("postReports"));
            assertFalse(response.body().containsKey("commentReports"));
        }

        @Test
        @Order(3)
        @DisplayName("Positive: Admin can filter comment reports only")
        void adminCanFilterCommentReportsOnly() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?type=comment")
                    .bearerAuth(adminToken),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            assertTrue(response.body().containsKey("commentReports"));
            assertFalse(response.body().containsKey("postReports"));
        }

        @Test
        @Order(4)
        @DisplayName("Negative: Regular user cannot list reports")
        void regularUserCannotListReports() {
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
        @Order(5)
        @DisplayName("Negative: Unauthenticated user cannot list reports")
        void unauthenticatedUserCannotListReports() {
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
}
