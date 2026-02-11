package com.anonymous.wall.admin.controller;

import com.anonymous.wall.entity.School;
import com.anonymous.wall.entity.SchoolDomain;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import com.anonymous.wall.service.SchoolService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Admin School Controller Tests")
class AdminSchoolControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    SchoolService schoolService;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/admin/schools";

    private UserEntity adminUser;
    private UserEntity regularUser;
    
    private String adminToken;
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

        // Create regular user
        regularUser = new UserEntity();
        regularUser.setEmail("user" + System.currentTimeMillis() + "@test.edu");
        regularUser.setSchoolDomain("test.edu");
        regularUser.setVerified(true);
        regularUser.setPasswordSet(true);
        regularUser.setRole("USER");
        regularUser = userRepository.save(regularUser);
        userToken = jwtTokenService.generateToken(regularUser);
    }

    @AfterEach
    void tearDown() {
        // Clean up test users
        if (adminUser != null && adminUser.getId() != null) {
            userRepository.deleteById(adminUser.getId());
        }
        if (regularUser != null && regularUser.getId() != null) {
            userRepository.deleteById(regularUser.getId());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should list all schools as admin")
    void shouldListAllSchools() {
        HttpRequest<?> request = HttpRequest.GET(BASE_PATH)
                .bearerAuth(adminToken);

        HttpResponse<List> response = client.toBlocking().exchange(request, List.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().size() > 0, "Should have seeded schools");
    }

    @Test
    @Order(2)
    @DisplayName("Should deny access to regular user")
    void shouldDenyAccessToRegularUser() {
        HttpRequest<?> request = HttpRequest.GET(BASE_PATH)
                .bearerAuth(userToken);

        assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request, List.class)
        );
    }

    @Test
    @Order(3)
    @DisplayName("Should create a new school with domains")
    void shouldCreateNewSchool() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "Test University " + System.currentTimeMillis());
        requestBody.put("domains", List.of("test-uni.edu", "test.edu.example"));

        HttpRequest<?> request = HttpRequest.POST(BASE_PATH, requestBody)
                .bearerAuth(adminToken);

        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().containsKey("id"));
        assertTrue(response.body().containsKey("message"));
    }

    @Test
    @Order(4)
    @DisplayName("Should reject creating school with duplicate name")
    void shouldRejectDuplicateSchoolName() {
        String schoolName = "Duplicate School " + System.currentTimeMillis();
        
        // Create first school
        Map<String, Object> requestBody1 = new HashMap<>();
        requestBody1.put("name", schoolName);
        requestBody1.put("domains", List.of("dup1.edu"));

        HttpRequest<?> request1 = HttpRequest.POST(BASE_PATH, requestBody1)
                .bearerAuth(adminToken);
        client.toBlocking().exchange(request1, Map.class);

        // Try to create second school with same name
        Map<String, Object> requestBody2 = new HashMap<>();
        requestBody2.put("name", schoolName);
        requestBody2.put("domains", List.of("dup2.edu"));

        HttpRequest<?> request2 = HttpRequest.POST(BASE_PATH, requestBody2)
                .bearerAuth(adminToken);

        assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request2, Map.class)
        );
    }

    @Test
    @Order(5)
    @DisplayName("Should reject creating school with duplicate domain")
    void shouldRejectDuplicateDomain() {
        String uniqueDomain = "unique-" + System.currentTimeMillis() + ".edu";
        
        // Create first school with domain
        Map<String, Object> requestBody1 = new HashMap<>();
        requestBody1.put("name", "School 1 " + System.currentTimeMillis());
        requestBody1.put("domains", List.of(uniqueDomain));

        HttpRequest<?> request1 = HttpRequest.POST(BASE_PATH, requestBody1)
                .bearerAuth(adminToken);
        client.toBlocking().exchange(request1, Map.class);

        // Try to create second school with same domain
        Map<String, Object> requestBody2 = new HashMap<>();
        requestBody2.put("name", "School 2 " + System.currentTimeMillis());
        requestBody2.put("domains", List.of(uniqueDomain));

        HttpRequest<?> request2 = HttpRequest.POST(BASE_PATH, requestBody2)
                .bearerAuth(adminToken);

        assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request2, Map.class)
        );
    }

    @Test
    @Order(6)
    @DisplayName("Should get school by ID")
    void shouldGetSchoolById() {
        // Get the list of schools first
        HttpRequest<?> listRequest = HttpRequest.GET(BASE_PATH)
                .bearerAuth(adminToken);

        HttpResponse<List> listResponse = client.toBlocking().exchange(listRequest, List.class);
        assertFalse(listResponse.body().isEmpty());
        
        Map<String, Object> firstSchool = (Map<String, Object>) listResponse.body().get(0);
        String schoolId = (String) firstSchool.get("id");

        // Get that specific school
        HttpRequest<?> getRequest = HttpRequest.GET(BASE_PATH + "/" + schoolId)
                .bearerAuth(adminToken);

        HttpResponse<Map> getResponse = client.toBlocking().exchange(getRequest, Map.class);

        assertEquals(HttpStatus.OK, getResponse.getStatus());
        assertNotNull(getResponse.body());
        assertEquals(schoolId, getResponse.body().get("id"));
        assertTrue(getResponse.body().containsKey("domains"));
    }

    @Test
    @Order(7)
    @DisplayName("Should add domain to school")
    void shouldAddDomainToSchool() {
        // Create a school first
        String schoolName = "Domain Test School " + System.currentTimeMillis();
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("name", schoolName);
        createRequest.put("domains", List.of("initial.edu"));

        HttpRequest<?> createReq = HttpRequest.POST(BASE_PATH, createRequest)
                .bearerAuth(adminToken);
        HttpResponse<Map> createResponse = client.toBlocking().exchange(createReq, Map.class);
        String schoolId = (String) createResponse.body().get("id");

        // Add a new domain
        Map<String, String> addDomainRequest = new HashMap<>();
        addDomainRequest.put("domain", "additional-" + System.currentTimeMillis() + ".edu");

        HttpRequest<?> addReq = HttpRequest.POST(BASE_PATH + "/" + schoolId + "/domains", addDomainRequest)
                .bearerAuth(adminToken);

        HttpResponse<Map> addResponse = client.toBlocking().exchange(addReq, Map.class);

        assertEquals(HttpStatus.OK, addResponse.getStatus());
        assertNotNull(addResponse.body());
        assertTrue(addResponse.body().containsKey("domain"));
    }

    @Test
    @Order(8)
    @DisplayName("Should delete school")
    void shouldDeleteSchool() {
        // Create a school first
        String schoolName = "Delete Test School " + System.currentTimeMillis();
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("name", schoolName);
        createRequest.put("domains", List.of("delete-test.edu"));

        HttpRequest<?> createReq = HttpRequest.POST(BASE_PATH, createRequest)
                .bearerAuth(adminToken);
        HttpResponse<Map> createResponse = client.toBlocking().exchange(createReq, Map.class);
        String schoolId = (String) createResponse.body().get("id");

        // Delete the school
        HttpRequest<?> deleteReq = HttpRequest.DELETE(BASE_PATH + "/" + schoolId)
                .bearerAuth(adminToken);

        HttpResponse<Map> deleteResponse = client.toBlocking().exchange(deleteReq, Map.class);

        assertEquals(HttpStatus.OK, deleteResponse.getStatus());
        assertNotNull(deleteResponse.body());
        assertTrue(deleteResponse.body().containsKey("message"));

        // Verify school is deleted
        HttpRequest<?> getReq = HttpRequest.GET(BASE_PATH + "/" + schoolId)
                .bearerAuth(adminToken);

        assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(getReq, Map.class)
        );
    }
}
