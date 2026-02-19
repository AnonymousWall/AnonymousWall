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
            assertNotNull(body.getDeadline());
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
            internship1.setSchoolDomain("test.edu");
            Internship internship2 = new Internship(testUser.getId(), "Microsoft", "PM Intern",
                null, null, null, null);
            internship2.setSchoolDomain("test.edu");
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
                internship.setSchoolDomain("test.edu");
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

        @Test
        @DisplayName("Should support sortBy parameter")
        void shouldSupportSortByParameter() {
            // Create internships with different timestamps
            Internship internship1 = new Internship(testUser.getId(), "Google", "SWE", null, null, null, null);
            internship1.setSchoolDomain("test.edu");
            Internship internship2 = new Internship(testUser.getId(), "Microsoft", "PM", null, null, null, null);
            internship2.setSchoolDomain("test.edu");
            
            internship1.setCreatedAt(internship1.getCreatedAt().minusSeconds(1));
            internshipRepository.save(internship1);
            internshipRepository.save(internship2);

            // Test newest (default)
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=newest")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> data = (List<Map>) response.body().get("data");
            assertEquals("Microsoft", data.get(0).get("company"));

            // Test oldest
            HttpResponse<Map> response2 = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "?sortBy=oldest")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response2.getStatus());
            List<Map> data2 = (List<Map>) response2.body().get("data");
            assertEquals("Google", data2.get(0).get("company"));
        }
    }

    @Nested
    @DisplayName("Get Internship By ID - GET /internships/{id}")
    class GetInternshipByIdTests {

        @Test
        @DisplayName("Should get internship by ID")
        void shouldGetInternshipById() {
            // Create internship
            Internship internship = new Internship(testUser.getId(), "Google", "SWE Intern",
                "$8000/month", "Mountain View, CA", "Great opportunity", LocalDate.of(2026, 6, 30));
            internship.setSchoolDomain("test.edu");
            final Internship savedInternship = internshipRepository.save(internship);

            HttpResponse<InternshipDTO> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + savedInternship.getId())
                    .header("Authorization", "Bearer " + jwtToken),
                InternshipDTO.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            InternshipDTO body = response.body();
            assertNotNull(body);
            assertEquals("Google", body.getCompany());
            assertEquals("SWE Intern", body.getRole());
            assertEquals("$8000/month", body.getSalary());
        }

        @Test
        @DisplayName("Should return 404 when internship not found")
        void shouldReturn404WhenInternshipNotFound() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken),
                    InternshipDTO.class
                )
            );
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Hide Internship - PATCH /internships/{id}/hide")
    class HideInternshipTests {

        @Test
        @DisplayName("Should hide internship successfully")
        void shouldHideInternshipSuccessfully() {
            // Create internship
            Internship internship = new Internship(testUser.getId(), "Google", "SWE Intern",
                null, null, null, null);
            internship.setSchoolDomain("test.edu");
            final Internship savedInternship = internshipRepository.save(internship);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PATCH(BASE_PATH + "/" + savedInternship.getId() + "/hide", null)
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            assertNotNull(body);
            assertTrue(body.get("message").toString().contains("hidden successfully"));

            // Verify internship is hidden
            Internship hidden = internshipRepository.findById(savedInternship.getId()).orElseThrow();
            assertTrue(hidden.isHidden());
        }

        @Test
        @DisplayName("Should return 404 when internship not found")
        void shouldReturn404WhenInternshipNotFound() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + java.util.UUID.randomUUID() + "/hide", null)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 403 when not owner")
        void shouldReturn403WhenNotOwner() {
            // Create another user
            UserEntity otherUser = new UserEntity();
            otherUser.setEmail("other" + System.currentTimeMillis() + "@test.edu");
            otherUser.setSchoolDomain("test.edu");
            otherUser.setVerified(true);
            otherUser.setPasswordSet(true);
            otherUser = userRepository.save(otherUser);

            // Create internship with otherUser
            Internship internship = new Internship(otherUser.getId(), "Google", "SWE Intern",
                null, null, null, null);
            internship.setSchoolDomain("test.edu");
            final Internship savedInternship = internshipRepository.save(internship);

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + savedInternship.getId() + "/hide", null)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Unhide Internship - PATCH /internships/{id}/unhide")
    class UnhideInternshipTests {

        @Test
        @DisplayName("Should unhide internship successfully")
        void shouldUnhideInternshipSuccessfully() {
            // Create and hide internship
            Internship internship = new Internship(testUser.getId(), "Google", "SWE Intern",
                null, null, null, null);
            internship.setSchoolDomain("test.edu");
            internship.setHidden(true);
            final Internship savedInternship = internshipRepository.save(internship);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PATCH(BASE_PATH + "/" + savedInternship.getId() + "/unhide", null)
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            assertNotNull(body);
            assertTrue(body.get("message").toString().contains("unhidden successfully"));

            // Verify internship is not hidden
            Internship unhidden = internshipRepository.findById(savedInternship.getId()).orElseThrow();
            assertFalse(unhidden.isHidden());
        }

        @Test
        @DisplayName("Should return 404 when internship not found")
        void shouldReturn404WhenInternshipNotFound() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + java.util.UUID.randomUUID() + "/unhide", null)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }
    }
}
