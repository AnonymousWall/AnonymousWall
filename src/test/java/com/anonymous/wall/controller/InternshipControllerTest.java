package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.InternshipDTO;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Internship Controller Tests")
class InternshipControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    InternshipRepository internshipRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/internships";

    private UserEntity testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new UserEntity();
        testUser.setEmail("recruiter" + System.currentTimeMillis() + "@test.edu");
        testUser.setSchoolDomain("test.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser.setProfileName("TestRecruiter");
        testUser = userRepository.save(testUser);
        jwtToken = jwtTokenService.generateToken(testUser);

        internshipRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        internshipRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create Internship - POST /internships")
    class CreateInternshipTests {

        @Test
        @DisplayName("Should create internship with all fields")
        void shouldCreateInternshipWithAllFields() {
            Map<String, Object> request = new HashMap<>();
            request.put("company", "Google");
            request.put("role", "Software Engineer Intern");
            request.put("salary", "$8000/month");
            request.put("location", "Mountain View, CA");
            request.put("description", "Work on cutting-edge projects");
            request.put("deadline", "2026-06-30");

            HttpResponse<InternshipDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + jwtToken),
                InternshipDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            InternshipDTO body = response.body();
            assertNotNull(body);
            assertEquals("Google", body.getCompany());
            assertEquals("Software Engineer Intern", body.getRole());
            assertEquals("$8000/month", body.getSalary());
            assertEquals("Mountain View, CA", body.getLocation());
            assertEquals("Work on cutting-edge projects", body.getDescription());
            assertEquals(LocalDate.of(2026, 6, 30), body.getDeadline());
            assertNotNull(body.getAuthor());
            assertNotNull(body.getCreatedAt());
            assertNotNull(body.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create internship with minimum required fields")
        void shouldCreateInternshipWithMinimumFields() {
            Map<String, Object> request = new HashMap<>();
            request.put("company", "Microsoft");
            request.put("role", "Data Science Intern");

            HttpResponse<InternshipDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, request)
                    .header("Authorization", "Bearer " + jwtToken),
                InternshipDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            InternshipDTO body = response.body();
            assertNotNull(body);
            assertEquals("Microsoft", body.getCompany());
            assertEquals("Data Science Intern", body.getRole());
            assertNull(body.getSalary());
            assertNull(body.getLocation());
            assertNull(body.getDescription());
            assertNull(body.getDeadline());
        }

        @Test
        @DisplayName("Should fail when company is missing")
        void shouldFailWhenCompanyMissing() {
            Map<String, Object> request = new HashMap<>();
            request.put("role", "Engineer");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request)
                        .header("Authorization", "Bearer " + jwtToken),
                    InternshipDTO.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when role is missing")
        void shouldFailWhenRoleMissing() {
            Map<String, Object> request = new HashMap<>();
            request.put("company", "Google");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request)
                        .header("Authorization", "Bearer " + jwtToken),
                    InternshipDTO.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when not authenticated")
        void shouldFailWhenNotAuthenticated() {
            Map<String, Object> request = new HashMap<>();
            request.put("company", "Google");
            request.put("role", "Engineer");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request),
                    InternshipDTO.class
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("List Internships - GET /internships")
    class ListInternshipsTests {

        @Test
        @DisplayName("Should list all internships")
        void shouldListAllInternships() {
            // Create test internships
            Internship internship1 = new Internship(testUser.getId(), "Google", "SWE Intern", 
                null, null, null, null);
            Internship internship2 = new Internship(testUser.getId(), "Microsoft", "PM Intern",
                null, null, null, null);
            internshipRepository.save(internship1);
            internshipRepository.save(internship2);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH)
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            assertNotNull(body);
            assertTrue(body.containsKey("data"));
            assertTrue(body.containsKey("pagination"));

            List<Map> data = (List<Map>) body.get("data");
            assertEquals(2, data.size());
        }

        @Test
        @DisplayName("Should return empty list when no internships exist")
        void shouldReturnEmptyListWhenNoInternships() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH)
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertTrue(data.isEmpty());
        }

        @Test
        @DisplayName("Should paginate results")
        void shouldPaginateResults() {
            // Create multiple internships
            for (int i = 1; i <= 5; i++) {
                Internship internship = new Internship(testUser.getId(), "Company" + i, "Role" + i,
                    null, null, null, null);
                internshipRepository.save(internship);
            }

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?page=1&limit=2")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(2, data.size());

            Map pagination = (Map) body.get("pagination");
            assertEquals(1, pagination.get("page"));
            assertEquals(2, pagination.get("limit"));
            assertEquals(5L, ((Number) pagination.get("total")).longValue());
        }

        @Test
        @DisplayName("Should fail when not authenticated")
        void shouldFailWhenNotAuthenticated() {
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
