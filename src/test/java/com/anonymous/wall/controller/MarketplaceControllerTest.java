package com.anonymous.wall.controller;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.ItemDTO;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.multipart.MultipartBody;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    // Minimal valid JPEG bytes (2x2 image)
    private static final byte[] MINIMAL_JPEG = {
        (byte)0xFF,(byte)0xD8,(byte)0xFF,(byte)0xE0,0x00,0x10,0x4A,0x46,0x49,0x46,0x00,0x01,
        0x01,0x00,0x00,0x01,0x00,0x01,0x00,0x00,(byte)0xFF,(byte)0xDB,0x00,0x43,0x00,0x08,
        0x06,0x06,0x07,0x06,0x05,0x08,0x07,0x07,0x07,0x09,0x09,0x08,0x0A,0x0C,0x14,0x0D,
        0x0C,0x0B,0x0B,0x0C,0x19,0x12,0x13,0x0F,0x14,0x1D,0x1A,(byte)0xFF,(byte)0xC0,0x00,
        0x0B,0x08,0x00,0x02,0x00,0x02,0x01,0x01,0x11,0x00,(byte)0xFF,(byte)0xC4,0x00,0x1F,
        0x00,0x00,0x01,0x05,0x01,0x01,0x01,0x01,0x01,0x01,0x00,0x00,0x00,0x00,0x00,0x00,
        0x00,0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,(byte)0xFF,(byte)0xDA,
        0x00,0x08,0x01,0x01,0x00,0x00,0x3F,0x00,(byte)0xFB,0x28,(byte)0xA2,(byte)0x8A,(byte)0xFF,(byte)0xD9
    };

    private UserEntity testUser;
    private UserEntity otherUser;
    private String jwtToken;
    private String otherJwtToken;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new UserEntity();
        testUser.setEmail("seller" + System.currentTimeMillis() + "@test.edu");
        testUser.setSchoolDomain("test.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser.setProfileName("TestSeller");
        testUser = userRepository.save(testUser);
        jwtToken = jwtTokenService.generateToken(testUser);

        // Create another user for ownership tests
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

    private MultipartBody multipartItem(String title, String price, String description, String condition, String category) {
        MultipartBody.Builder builder = MultipartBody.builder();
        if (title != null) builder.addPart("title", title);
        if (price != null) builder.addPart("price", price);
        if (description != null) builder.addPart("description", description);
        if (condition != null) builder.addPart("condition", condition);
        if (category != null) builder.addPart("category", category);
        return builder.build();
    }

    private MultipartBody multipartItem(String title, String price) {
        return multipartItem(title, price, null, null, null);
    }

    @Nested
    @DisplayName("Create Item - POST /marketplace")
    class CreateItemTests {

        @Test
        @DisplayName("Should create item with all fields")
        void shouldCreateItemWithAllFields() {
            MultipartBody body = multipartItem("MacBook Pro", "1500.00", "Excellent condition, barely used", "like-new", "electronics");

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, body)
                    .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                    .header("Authorization", "Bearer " + jwtToken),
                ItemDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            ItemDTO body2 = response.body();
            assertNotNull(body2);
            assertEquals("MacBook Pro", body2.getTitle());
            assertEquals("Excellent condition, barely used", body2.getDescription());
            assertNotNull(body2.getPrice());
            assertNotNull(body2.getCategory());
            assertEquals("electronics", body2.getCategory().getValue());
            assertEquals("like-new", body2.getCondition().getValue());
            assertNotNull(body2.getAuthor());
            assertEquals("TestSeller", body2.getAuthor().getProfileName());
            assertFalse(body2.getAuthor().getIsAnonymous());
            assertNotNull(body2.getCreatedAt());
            assertNotNull(body2.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create item with minimum required fields")
        void shouldCreateItemWithMinimumFields() {
            MultipartBody body = multipartItem("Textbook", "25.00");

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, body)
                    .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
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
        @DisplayName("Should return imageUrls in response when created with images")
        void shouldReturnImageUrlsWhenCreatedWithImages() {
            MultipartBody body = MultipartBody.builder()
                .addPart("title", "Item with image")
                .addPart("price", "99.00")
                .addPart("images", "test.jpg", MediaType.IMAGE_JPEG_TYPE, MINIMAL_JPEG)
                .build();

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, body)
                    .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                    .header("Authorization", "Bearer " + jwtToken),
                ItemDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            ItemDTO responseBody = response.body();
            assertNotNull(responseBody);
            assertNotNull(responseBody.getImageUrls());
            assertFalse(responseBody.getImageUrls().isEmpty());
        }

        @Test
        @DisplayName("Should return null imageUrls when no images uploaded")
        void shouldReturnNullImageUrlsWhenNoImagesUploaded() {
            MultipartBody body = multipartItem("Item without images", "50.00");

            HttpResponse<ItemDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH, body)
                    .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                    .header("Authorization", "Bearer " + jwtToken),
                ItemDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            ItemDTO responseBody = response.body();
            assertNotNull(responseBody);
            assertNull(responseBody.getImageUrls());
        }

        @Test
        @DisplayName("Should fail when title is missing")
        void shouldFailWhenTitleMissing() {
            MultipartBody body = multipartItem(null, "100.00");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, body)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .header("Authorization", "Bearer " + jwtToken),
                    ItemDTO.class
                )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when price is missing")
        void shouldFailWhenPriceMissing() {
            MultipartBody body = multipartItem("Item", null);

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, body)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .header("Authorization", "Bearer " + jwtToken),
                    ItemDTO.class
                )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail when price is negative")
        void shouldFailWhenPriceNegative() {
            MultipartBody body = multipartItem("Item", "-10.00");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, body)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .header("Authorization", "Bearer " + jwtToken),
                    ItemDTO.class
                )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() {
            MultipartBody body = multipartItem("Item", "100.00");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH, body)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE),
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
            // Create an item
            MarketplaceItem item = new MarketplaceItem();
            item.setUserId(testUser.getId());
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
            // Create multiple items
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
            // Create items with different prices
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
            // Create electronics items
            for (int i = 1; i <= 3; i++) {
                MarketplaceItem item = new MarketplaceItem();
                item.setUserId(testUser.getId());
                item.setSchoolDomain("test.edu");
                item.setTitle("Electronics " + i);
                item.setPrice(new BigDecimal(i * 100));
                item.setCategory("electronics");
                marketplaceItemRepository.save(item);
            }
            // Create a textbooks item
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
            // Verify all returned items are electronics
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
            // Create an item
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
            // Create item owned by testUser
            MarketplaceItem item = new MarketplaceItem();
            item.setUserId(testUser.getId());
            item.setSchoolDomain("test.edu");
            item.setTitle("Item");
            item.setPrice(new BigDecimal("100.00"));
            MarketplaceItem savedItem = marketplaceItemRepository.save(item);

            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("title", "Hacked Title");

            // Try to update with otherUser's token
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
