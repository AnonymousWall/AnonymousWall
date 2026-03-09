package com.anonymous.wall.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.model.CreateItemRequestCategory;
import com.anonymous.wall.model.CreateItemRequestCondition;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.MarketplaceService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("MarketplaceServiceImpl - Create Item Tests")
class MarketplaceServiceImplCreateItemTest {

    @Inject
    private MarketplaceService marketplaceService;

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Clean up
        marketplaceItemRepository.deleteAll();

        // Create test user
        testUser = new UserEntity();
        testUser.setEmail("testuser" + System.currentTimeMillis() + "@test.edu");
        testUser.setSchoolDomain("test.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        marketplaceItemRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create Item - Positive Cases")
    class CreateItemPositiveTests {

        @Test
        @DisplayName("Should create item with all fields")
        void shouldCreateItemWithAllFields() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("Test Item", 99.99f);
            request.setDescription("Description");
            request.setCategory(CreateItemRequestCategory.ELECTRONICS);
            request.setCondition(CreateItemRequestCondition.NEW);

            // Act
            MarketplaceItem result = marketplaceService.createItem(request, null, testUser.getId());

            // Assert
            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("Test Item", result.getTitle());
            assertEquals("Description", result.getDescription());
            // Compare with 2 decimal places (matching DECIMAL(10,2) in database)
            assertEquals(0, new BigDecimal("99.99").compareTo(result.getPrice().setScale(2, RoundingMode.HALF_UP)));
            assertEquals("electronics", result.getCategory());
            assertEquals("new", result.getCondition());
            assertEquals(testUser.getId(), result.getUserId());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create item with minimum required fields")
        void shouldCreateItemWithMinimumFields() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("Min Item", null);
            request.setPrice(0f);

            // Act
            MarketplaceItem result = marketplaceService.createItem(request, null, testUser.getId());

            // Assert
            assertNotNull(result);
            assertEquals("Min Item", result.getTitle());
            assertNull(result.getDescription());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getPrice()));
            assertNull(result.getCategory());
            assertNull(result.getCondition());
        }

        @Test
        @DisplayName("Should create item with zero price")
        void shouldCreateItemWithZeroPrice() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("Free Item", 0f);
            request.setDescription("Free stuff");

            // Act
            MarketplaceItem result = marketplaceService.createItem(request, null, testUser.getId());

            // Assert
            assertNotNull(result);
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getPrice()));
        }

        @Test
        @DisplayName("Should create item with each valid condition")
        void shouldCreateItemWithValidConditions() {
            for (CreateItemRequestCondition condition : CreateItemRequestCondition.values()) {
                // Arrange
                CreateItemRequest request = new CreateItemRequest("Item " + condition.getValue(), 10f);
                request.setDescription("Test");
                request.setCondition(condition);

                // Act
                MarketplaceItem result = marketplaceService.createItem(request, null, testUser.getId());

                // Assert
                assertNotNull(result);
                assertEquals(condition.getValue(), result.getCondition());
            }
        }

        @Test
        @DisplayName("Should trim title whitespace")
        void shouldTrimTitleWhitespace() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("  Trimmed Title  ", 10f);
            request.setDescription("Description");

            // Act
            MarketplaceItem result = marketplaceService.createItem(request, null, testUser.getId());

            // Assert
            assertEquals("Trimmed Title", result.getTitle());
        }
    }

    @Nested
    @DisplayName("Create Item - Negative Cases")
    class CreateItemNegativeTests {

        @Test
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("Test", 10f);
            request.setDescription("Description");
            UUID nonExistentUserId = UUID.randomUUID();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                marketplaceService.createItem(request, null, nonExistentUserId);
            });
            assertTrue(exception.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("Should fail when title is null")
        void shouldFailWhenTitleIsNull() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest(null, 10f);
            request.setDescription("Description");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                marketplaceService.createItem(request, null, testUser.getId());
            });
            assertTrue(exception.getMessage().contains("Title is required"));
        }

        @Test
        @DisplayName("Should fail when title is empty")
        void shouldFailWhenTitleIsEmpty() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("   ", 10f);
            request.setDescription("Description");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                marketplaceService.createItem(request, null, testUser.getId());
            });
            assertTrue(exception.getMessage().contains("Title is required"));
        }

        @Test
        @DisplayName("Should fail when price is null")
        void shouldFailWhenPriceIsNull() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("Test", 0f);
            request.setDescription("Description");
            request.setPrice(null);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                marketplaceService.createItem(request, null, testUser.getId());
            });
            assertTrue(exception.getMessage().contains("Price is required"));
        }

        @Test
        @DisplayName("Should fail when price is negative")
        void shouldFailWhenPriceIsNegative() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("Test", -1f);
            request.setDescription("Description");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                marketplaceService.createItem(request, null, testUser.getId());
            });
            assertTrue(exception.getMessage().contains("Price must be greater than or equal to 0"));
        }
    }

    @Nested
    @DisplayName("Create Item - Boundary Cases")
    class CreateItemBoundaryTests {

        @Test
        @DisplayName("Should create item with exactly 255 character title")
        void shouldCreateItemWithMaxLengthTitle() {
            // Arrange
            String maxTitle = "a".repeat(255);
            CreateItemRequest request = new CreateItemRequest(maxTitle, 10f);
            request.setDescription("Description");

            // Act
            MarketplaceItem result = marketplaceService.createItem(request, null, testUser.getId());

            // Assert
            assertNotNull(result);
            assertEquals(255, result.getTitle().length());
        }

        @Test
        @DisplayName("Should fail when title exceeds 255 characters")
        void shouldFailWhenTitleTooLong() {
            // Arrange
            String tooLongTitle = "a".repeat(256);
            CreateItemRequest request = new CreateItemRequest(tooLongTitle, 10f);
            request.setDescription("Description");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                marketplaceService.createItem(request, null, testUser.getId());
            });
            assertTrue(exception.getMessage().contains("Title cannot exceed 255 characters"));
        }

        @Test
        @DisplayName("Should create item with one character title")
        void shouldCreateItemWithOneCharTitle() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("A", 10f);
            request.setDescription("Description");

            // Act
            MarketplaceItem result = marketplaceService.createItem(request, null, testUser.getId());

            // Assert
            assertNotNull(result);
            assertEquals("A", result.getTitle());
        }

        @Test
        @DisplayName("Should create item with large price")
        void shouldCreateItemWithLargePrice() {
            // Arrange - Use a value well within DECIMAL(10,2) limits (max: 99999999.99)
            CreateItemRequest request = new CreateItemRequest("Expensive", 9999999.99f);
            request.setDescription("Very expensive");

            // Act
            MarketplaceItem result = marketplaceService.createItem(request, null, testUser.getId());

            // Assert
            assertNotNull(result);
            assertTrue(result.getPrice().compareTo(BigDecimal.valueOf(9999999.0)) > 0);
        }
    }
}
