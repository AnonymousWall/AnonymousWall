package com.anonymous.wall.controller;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.model.CreateItemRequestCondition;
import com.anonymous.wall.model.ItemDTO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Marketplace Controller Tests")
class MarketplaceControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/marketplace";

    private UserEntity testUser;
    private UserEntity otherUser;
    private String jwtToken;
    private String otherJwtToken;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setEmail("seller" + System.currentTimeMillis() + "@test.edu");
        testUser.setSchoolDomain("test.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser.setProfileName("TestSeller");
        testUser = userRepository.save(testUser);
        jwtToken = jwtTokenService.generateToken(testUser);

        otherUser = new UserEntity();
        otherUser.setEmail("buyer" + System.currentTimeMillis() + "@test.edu");
        otherUser.setSchoolDomain("test.edu");
        otherUser.setVerified(true);
        otherUser.setPasswordSet(true);
        otherUser.setProfileName("TestBuyer");
        otherUser = userRepository.save(otherUser);
        otherJwtToken = jwtTokenService.generateToken(otherUser);

        marketplaceItemRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        marketplaceItemRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create Item - POST /marketplace")
    class CreateItemTests {

        @Test
        @DisplayName("Should create item with all fields")
        void shouldCreateItemWithAllFields() {
            CreateItemRequest request = new CreateItemRequest("MacBook Pro", 1500.00f);
            request.setDescription("Excellent condition, barely used");
            request.setCondition(CreateItemRequestCondition.LIKE_NEW);
            request.setCategory(com.anonymous.wall.model.CreateItemRequestCategory.ELECTRONICS);

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request)
                            .header("Authorization", "Bearer " + jwtToken),
                    ItemDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            ItemDTO body = response.body();
            assertNotNull(body);
            assertEquals("MacBook Pro", body.getTitle());
            assertEquals("Excellent condition, barely used", body.getDescription());
            assertNotNull(body.getPrice());
            assertNotNull(body.getCategory());
            assertEquals("electronics", body.getCategory().getValue());
            assertEquals("like-new", body.getCondition().getValue());
            assertNotNull(body.getAuthor());
            assertEquals("TestSeller", body.getAuthor().getProfileName());
            assertFalse(body.getAuthor().getIsAnonymous());
            assertNotNull(body.getCreatedAt());
            assertNotNull(body.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create item with minimum required fields")
        void shouldCreateItemWithMinimumFields() {
            CreateItemRequest request = new CreateItemRequest("Textbook", 25.00f);

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request)
                            .header("Authorization", "Bearer " + jwtToken),
                    ItemDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            ItemDTO responseBody = response.body();
            assertNotNull(responseBody);
            assertEquals("Textbook", responseBody.getTitle());
            assertNull(responseBody.getDescription());
            assertNull(responseBody.getCategory());
            assertNull(responseBody.getCondition());
        }

        @Test
        @DisplayName("Should attach imageObjectNames to saved item")
        void shouldReturnImageUrlsWhenCreatedWithImages() {
            CreateItemRequest request = new CreateItemRequest("Item with image", 99.00f);
            request.setImageObjectNames(List.of("marketplace/uuid1.jpg"));

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request)
                            .header("Authorization", "Bearer " + jwtToken),
                    ItemDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            ItemDTO responseBody = response.body();
            assertNotNull(responseBody);
            assertNotNull(responseBody.getImageUrls());
            assertFalse(responseBody.getImageUrls().isEmpty());
            assertEquals("marketplace/uuid1.jpg", responseBody.getImageUrls().get(0));
        }

        @Test
        @DisplayName("Should return empty imageUrls when no imageObjectNames provided")
        void shouldReturnNullImageUrlsWhenNoImagesUploaded() {
            CreateItemRequest request = new CreateItemRequest("Item without images", 50.00f);

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, request)
                            .header("Authorization", "Bearer " + jwtToken),
                    ItemDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            ItemDTO responseBody = response.body();
            assertNotNull(responseBody);
            assertTrue(responseBody.getImageUrls() == null || responseBody.getImageUrls().isEmpty());
        }

        @Test
        @DisplayName("Should fail when title is missing")
        void shouldFailWhenTitleMissing() {
            CreateItemRequest request = new CreateItemRequest(null, 100.00f);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, request)
                                    .header("Authorization", "Bearer " + jwtToken),
                            ItemDTO.class
                    )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when price is missing")
        void shouldFailWhenPriceMissing() {
            CreateItemRequest request = new CreateItemRequest("Item", null);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, request)
                                    .header("Authorization", "Bearer " + jwtToken),
                            ItemDTO.class
                    )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when price is negative")
        void shouldFailWhenPriceNegative() {
            CreateItemRequest request = new CreateItemRequest("Item", -10.00f);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, request)
                                    .header("Authorization", "Bearer " + jwtToken),
                            ItemDTO.class
                    )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() {
            CreateItemRequest request = new CreateItemRequest("Item", 100.00f);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH, request),
                            ItemDTO.class
                    )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Get Item by ID - GET /marketplace/{itemId}")
    class GetItemTests {

        @Test
        @DisplayName("Should get item by ID")
        void shouldGetItemById() {
            MarketplaceItem item = new MarketplaceItem();
            item.setUserId(testUser.getId());
            item.setProfileName(testUser.getProfileName());
            item.setSchoolDomain("test.edu");
            item.setTitle("Test Item");
            item.setDescription("Description");
            item.setPrice(new BigDecimal("50.00"));
            item.setCategory("textbooks");
            item.setCondition("good");
            item = marketplaceItemRepository.save(item);

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + item.getId())
                            .header("Authorization", "Bearer " + jwtToken),
                    ItemDTO.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            ItemDTO body = response.body();
            assertNotNull(body);
            assertEquals("Test Item", body.getTitle());
            assertEquals("Description", body.getDescription());
            assertEquals("textbooks", body.getCategory().getValue());
            assertEquals("good", body.getCondition().getValue());
            assertEquals("TestSeller", body.getAuthor().getProfileName());
        }

        @Test
        @DisplayName("Should return 404 for non-existent item")
        void shouldReturn404ForNonExistentItem() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.GET(BASE_PATH + "/00000000-0000-0000-0000-000000000000")
                                    .header("Authorization", "Bearer " + jwtToken),
                            ItemDTO.class
                    )
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() {
            MarketplaceItem item = new MarketplaceItem();
            item.setUserId(testUser.getId());
            item.setSchoolDomain("test.edu");
            item.setTitle("Test Item");
            item.setPrice(new BigDecimal("50.00"));
            MarketplaceItem savedItem = marketplaceItemRepository.save(item);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.GET(BASE_PATH + "/" + savedItem.getId()),
                            ItemDTO.class
                    )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("List Items - GET /marketplace")
    class ListItemsTests {

        @Test
        @DisplayName("Should list items with pagination")
        void shouldListItems() {
            for (int i = 1; i <= 5; i++) {
                MarketplaceItem item = new MarketplaceItem();
                item.setUserId(testUser.getId());
                item.setSchoolDomain("test.edu");
                item.setTitle("Item " + i);
                item.setPrice(new BigDecimal(i * 10));
                marketplaceItemRepository.save(item);
            }

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "?page=1&limit=3")
                            .header("Authorization", "Bearer " + jwtToken),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            assertNotNull(body);
            assertTrue(body.containsKey("data"));
            assertTrue(body.containsKey("pagination"));

            List items = (List) body.get("data");
            assertEquals(3, items.size());

            Map pagination = (Map) body.get("pagination");
            assertEquals(1, pagination.get("page"));
            assertEquals(3, pagination.get("limit"));
            assertEquals(5, pagination.get("total"));
        }

        @Test
        @DisplayName("Should sort items by price")
        void shouldSortByPrice() {
            MarketplaceItem item1 = new MarketplaceItem();
            item1.setUserId(testUser.getId());
            item1.setSchoolDomain("test.edu");
            item1.setTitle("Expensive");
            item1.setPrice(new BigDecimal("500.00"));
            marketplaceItemRepository.save(item1);

            MarketplaceItem item2 = new MarketplaceItem();
            item2.setUserId(testUser.getId());
            item2.setSchoolDomain("test.edu");
            item2.setTitle("Cheap");
            item2.setPrice(new BigDecimal("10.00"));
            marketplaceItemRepository.save(item2);

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "?sortBy=price-asc")
                            .header("Authorization", "Bearer " + jwtToken),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            List items = (List) body.get("data");
            assertEquals(2, items.size());
        }

        @Test
        @DisplayName("Should filter items by category")
        void shouldFilterByCategory() {
            for (int i = 1; i <= 3; i++) {
                MarketplaceItem item = new MarketplaceItem();
                item.setUserId(testUser.getId());
                item.setSchoolDomain("test.edu");
                item.setTitle("Electronics " + i);
                item.setPrice(new BigDecimal(i * 100));
                item.setCategory("electronics");
                marketplaceItemRepository.save(item);
            }
            MarketplaceItem textbookItem = new MarketplaceItem();
            textbookItem.setUserId(testUser.getId());
            textbookItem.setSchoolDomain("test.edu");
            textbookItem.setTitle("Textbook");
            textbookItem.setPrice(new BigDecimal("25.00"));
            textbookItem.setCategory("textbooks");
            marketplaceItemRepository.save(textbookItem);

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "?category=electronics")
                            .header("Authorization", "Bearer " + jwtToken),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            List items = (List) body.get("data");
            assertEquals(3, items.size());
            for (Object obj : items) {
                Map itemMap = (Map) obj;
                assertEquals("electronics", itemMap.get("category"));
            }
        }

        @Test
        @DisplayName("Should return 400 for invalid category")
        void shouldReturn400ForInvalidCategory() {
            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.GET(BASE_PATH + "?category=invalid-category")
                                    .header("Authorization", "Bearer " + jwtToken),
                            Map.class
                    )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() {
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
    @DisplayName("Update Item - PUT /marketplace/{itemId}")
    class UpdateItemTests {

        @Test
        @DisplayName("Should update item title")
        void shouldUpdateTitle() {
            MarketplaceItem item = new MarketplaceItem();
            item.setUserId(testUser.getId());
            item.setSchoolDomain("test.edu");
            item.setTitle("Old Title");
            item.setPrice(new BigDecimal("100.00"));
            item = marketplaceItemRepository.save(item);

            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("title", "New Title");

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                    HttpRequest.PUT(BASE_PATH + "/" + item.getId(), updateRequest)
                            .header("Authorization", "Bearer " + jwtToken),
                    ItemDTO.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            ItemDTO body = response.body();
            assertNotNull(body);
            assertEquals("New Title", body.getTitle());
        }

        @Test
        @DisplayName("Should update item price")
        void shouldUpdatePrice() {
            MarketplaceItem item = new MarketplaceItem();
            item.setUserId(testUser.getId());
            item.setSchoolDomain("test.edu");
            item.setTitle("Item");
            item.setPrice(new BigDecimal("100.00"));
            item = marketplaceItemRepository.save(item);

            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("price", 150.00f);

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                    HttpRequest.PUT(BASE_PATH + "/" + item.getId(), updateRequest)
                            .header("Authorization", "Bearer " + jwtToken),
                    ItemDTO.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            ItemDTO body = response.body();
            assertNotNull(body);
            assertTrue(body.getPrice() > 100.0f);
        }

        @Test
        @DisplayName("Should update multiple fields")
        void shouldUpdateMultipleFields() {
            MarketplaceItem item = new MarketplaceItem();
            item.setUserId(testUser.getId());
            item.setSchoolDomain("test.edu");
            item.setTitle("Old Item");
            item.setPrice(new BigDecimal("100.00"));
            item.setDescription("Old description");
            item = marketplaceItemRepository.save(item);

            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("title", "Updated Item");
            updateRequest.put("description", "New description");
            updateRequest.put("price", 200.00f);

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                    HttpRequest.PUT(BASE_PATH + "/" + item.getId(), updateRequest)
                            .header("Authorization", "Bearer " + jwtToken),
                    ItemDTO.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            ItemDTO body = response.body();
            assertNotNull(body);
            assertEquals("Updated Item", body.getTitle());
            assertEquals("New description", body.getDescription());
        }

        @Test
        @DisplayName("Should fail when updating another user's item")
        void shouldFailWhenUpdatingOtherUsersItem() {
            MarketplaceItem item = new MarketplaceItem();
            item.setUserId(testUser.getId());
            item.setSchoolDomain("test.edu");
            item.setTitle("Item");
            item.setPrice(new BigDecimal("100.00"));
            MarketplaceItem savedItem = marketplaceItemRepository.save(item);

            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("title", "Hacked Title");

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.PUT(BASE_PATH + "/" + savedItem.getId(), updateRequest)
                                    .header("Authorization", "Bearer " + otherJwtToken),
                            ItemDTO.class
                    )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail with negative price")
        void shouldFailWithNegativePrice() {
            MarketplaceItem item = new MarketplaceItem();
            item.setUserId(testUser.getId());
            item.setSchoolDomain("test.edu");
            item.setTitle("Item");
            item.setPrice(new BigDecimal("100.00"));
            MarketplaceItem savedItem = marketplaceItemRepository.save(item);

            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("price", -50.00f);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.PUT(BASE_PATH + "/" + savedItem.getId(), updateRequest)
                                    .header("Authorization", "Bearer " + jwtToken),
                            ItemDTO.class
                    )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 404 for non-existent item")
        void shouldReturn404ForNonExistentItem() {
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("title", "New Title");

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.PUT(BASE_PATH + "/00000000-0000-0000-0000-000000000000", updateRequest)
                                    .header("Authorization", "Bearer " + jwtToken),
                            ItemDTO.class
                    )
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() {
            MarketplaceItem item = new MarketplaceItem();
            item.setUserId(testUser.getId());
            item.setSchoolDomain("test.edu");
            item.setTitle("Item");
            item.setPrice(new BigDecimal("100.00"));
            MarketplaceItem savedItem = marketplaceItemRepository.save(item);

            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("title", "New Title");

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.PUT(BASE_PATH + "/" + savedItem.getId(), updateRequest),
                            ItemDTO.class
                    )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }
}