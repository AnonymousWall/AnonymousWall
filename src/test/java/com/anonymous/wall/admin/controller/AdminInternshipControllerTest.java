package com.anonymous.wall.admin.controller;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.InternshipRepository;
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
@DisplayName("Admin Internship Controller Tests")
class AdminInternshipControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    InternshipRepository internshipRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/admin/internships";

    private UserEntity adminUser;
    private UserEntity regularUser;
    private Internship testInternship;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        internshipRepository.deleteAll();

        adminUser = new UserEntity();
        adminUser.setEmail("admin" + System.currentTimeMillis() + "@test.edu");
        adminUser.setSchoolDomain("test.edu");
        adminUser.setVerified(true);
        adminUser.setPasswordSet(true);
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);
        adminToken = jwtTokenService.generateToken(adminUser);

        regularUser = new UserEntity();
        regularUser.setEmail("user" + System.currentTimeMillis() + "@test.edu");
        regularUser.setSchoolDomain("test.edu");
        regularUser.setVerified(true);
        regularUser.setPasswordSet(true);
        regularUser.setRole("USER");
        regularUser = userRepository.save(regularUser);
        userToken = jwtTokenService.generateToken(regularUser);

        testInternship = new Internship();
        testInternship.setUserId(regularUser.getId());
        testInternship.setCompany("Test Corp");
        testInternship.setRole("Engineer");
        testInternship.setWall("campus");
        testInternship.setSchoolDomain("test.edu");
        testInternship.setHidden(false);
        testInternship = internshipRepository.save(testInternship);
    }

    @AfterEach
    void tearDown() {
        internshipRepository.deleteAll();
    }

    @Nested
    @DisplayName("List Internships Endpoint Tests")
    class ListInternshipsTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can list all internships")
        void adminCanListInternships() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH).bearerAuth(adminToken),
                Map.class
            );
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            List<?> data = (List<?>) response.body().get("data");
            assertNotNull(data);
        }

        @Test
        @Order(2)
        @DisplayName("Negative: Regular user cannot list internships via admin endpoint")
        void regularUserCannotListInternships() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH).bearerAuth(userToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }

        @Test
        @Order(3)
        @DisplayName("Negative: Unauthenticated user cannot list internships")
        void unauthenticatedCannotListInternships() {
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
    @DisplayName("Get Internship By ID Endpoint Tests")
    class GetByIdTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can get internship by ID")
        void adminCanGetInternshipById() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testInternship.getId()).bearerAuth(adminToken),
                Map.class
            );
            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals(testInternship.getId().toString(), response.body().get("id"));
        }
    }

    @Nested
    @DisplayName("Hide/Unhide Internship Endpoint Tests")
    class HideUnhideTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can hide an internship")
        void adminCanHideInternship() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PUT(BASE_PATH + "/" + testInternship.getId() + "/hide", null).bearerAuth(adminToken),
                Map.class
            );
            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue(response.body().get("message").toString().contains("hidden"));

            Internship updated = internshipRepository.findById(testInternship.getId()).orElseThrow();
            assertTrue(updated.isHidden());
        }

        @Test
        @Order(2)
        @DisplayName("Positive: Admin can unhide an internship")
        void adminCanUnhideInternship() {
            // First hide it
            testInternship.setHidden(true);
            internshipRepository.update(testInternship);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PUT(BASE_PATH + "/" + testInternship.getId() + "/unhide", null).bearerAuth(adminToken),
                Map.class
            );
            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue(response.body().get("message").toString().contains("unhidden"));

            Internship updated = internshipRepository.findById(testInternship.getId()).orElseThrow();
            assertFalse(updated.isHidden());
        }

        @Test
        @Order(3)
        @DisplayName("Negative: Regular user cannot hide an internship")
        void regularUserCannotHideInternship() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PUT(BASE_PATH + "/" + testInternship.getId() + "/hide", null).bearerAuth(userToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }
}
