package com.anonymous.wall.admin.controller;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.MarketplaceItemRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Admin Marketplace Controller Tests")
class AdminMarketplaceControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/admin/marketplaces";

    private UserEntity adminUser;
    private UserEntity regularUser;
    private MarketplaceItem testItem;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        marketplaceItemRepository.deleteAll();

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

        testItem = new MarketplaceItem();
        testItem.setUserId(regularUser.getId());
        testItem.setTitle("Test Item");
        testItem.setDescription("A test item");
        testItem.setPrice(new BigDecimal("10.00"));
        testItem.setCategory("books");
        testItem.setCondition("good");
        testItem.setWall("campus");
        testItem.setSchoolDomain("test.edu");
        testItem.setHidden(false);
        testItem = marketplaceItemRepository.save(testItem);
    }

    @AfterEach
    void tearDown() {
        marketplaceItemRepository.deleteAll();
    }

    @Nested
    @DisplayName("List Marketplace Items Endpoint Tests")
    class ListMarketplaceTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can list all marketplace items")
        void adminCanListMarketplaceItems() {
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
        @DisplayName("Negative: Regular user cannot list marketplace items via admin endpoint")
        void regularUserCannotListMarketplaceItems() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH).bearerAuth(userToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Get Marketplace Item By ID Endpoint Tests")
    class GetByIdTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can get marketplace item by ID")
        void adminCanGetMarketplaceById() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + testItem.getId()).bearerAuth(adminToken),
                Map.class
            );
            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals(testItem.getId().toString(), response.body().get("id"));
        }
    }

    @Nested
    @DisplayName("Hide/Unhide Marketplace Item Endpoint Tests")
    class HideUnhideTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can hide a marketplace item")
        void adminCanHideMarketplaceItem() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PUT(BASE_PATH + "/" + testItem.getId() + "/hide", null).bearerAuth(adminToken),
                Map.class
            );
            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue(response.body().get("message").toString().contains("hidden"));

            MarketplaceItem updated = marketplaceItemRepository.findById(testItem.getId()).orElseThrow();
            assertTrue(updated.isHidden());
        }

        @Test
        @Order(2)
        @DisplayName("Positive: Admin can unhide a marketplace item")
        void adminCanUnhideMarketplaceItem() {
            testItem.setHidden(true);
            marketplaceItemRepository.update(testItem);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PUT(BASE_PATH + "/" + testItem.getId() + "/unhide", null).bearerAuth(adminToken),
                Map.class
            );
            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue(response.body().get("message").toString().contains("unhidden"));

            MarketplaceItem updated = marketplaceItemRepository.findById(testItem.getId()).orElseThrow();
            assertFalse(updated.isHidden());
        }

        @Test
        @Order(3)
        @DisplayName("Negative: Regular user cannot hide a marketplace item")
        void regularUserCannotHideMarketplaceItem() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PUT(BASE_PATH + "/" + testItem.getId() + "/hide", null).bearerAuth(userToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }
}
