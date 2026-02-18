package com.anonymous.wall.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.model.CreateItemRequestCondition;
import com.anonymous.wall.model.UpdateItemRequest;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("MarketplaceServiceImpl - Update Item Tests")
class MarketplaceServiceImplUpdateItemTest {

    @Inject
    private MarketplaceService marketplaceService;

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUser;
    private UserEntity otherUser;
    private MarketplaceItem testItem;

    @BeforeEach
    void setUp() {
        // Clean up
        marketplaceItemRepository.deleteAll();

        // Create test users
        testUser = new UserEntity();
        testUser.setEmail("testuser" + System.currentTimeMillis() + "@test.edu");
        testUser.setSchoolDomain("test.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);

        otherUser = new UserEntity();
        otherUser.setEmail("otheruser" + System.currentTimeMillis() + "@test.edu");
        otherUser.setSchoolDomain("test.edu");
        otherUser.setVerified(true);
        otherUser.setPasswordSet(true);
        otherUser = userRepository.save(otherUser);

        // Create test item
        CreateItemRequest request = new CreateItemRequest("Original Title", 50f);
        request.setDescription("Original Description");
        request.setCategory("Books");
        request.setCondition(CreateItemRequestCondition.GOOD);
        testItem = marketplaceService.createItem(request, testUser.getId());
    }

    @AfterEach
    void tearDown() {
        marketplaceItemRepository.deleteAll();
    }

    @Nested
    @DisplayName("Update Item - Positive Cases")
    class UpdateItemPositiveTests {

        @Test
        @DisplayName("Should update title only")
        void shouldUpdateTitleOnly() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("New Title");

            // Act
            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            // Assert
            assertEquals("New Title", result.getTitle());
            assertEquals("Original Description", result.getDescription());
            assertEquals(0, BigDecimal.valueOf(50).compareTo(result.getPrice()));
        }

        @Test
        @DisplayName("Should update price only")
        void shouldUpdatePriceOnly() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setPrice(75.5f);

            // Act
            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            // Assert
            assertEquals("Original Title", result.getTitle());
            assertEquals(0, BigDecimal.valueOf(75.5).compareTo(result.getPrice()));
        }

        @Test
        @DisplayName("Should update sold status")
        void shouldUpdateSoldStatus() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setSold(true);

            // Act
            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            // Assert
            assertTrue(result.isSold());
            assertEquals("Original Title", result.getTitle());
        }

        @Test
        @DisplayName("Should update all fields")
        void shouldUpdateAllFields() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("Completely New");
            request.setDescription("New description");
            request.setPrice(100f);
            request.setCategory("Electronics");
            request.setCondition(CreateItemRequestCondition.LIKE_NEW);
            request.setSold(true);

            // Act
            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            // Assert
            assertEquals("Completely New", result.getTitle());
            assertEquals("New description", result.getDescription());
            assertEquals(0, BigDecimal.valueOf(100).compareTo(result.getPrice()));
            assertEquals("Electronics", result.getCategory());
            assertEquals("like-new", result.getCondition());
            assertTrue(result.isSold());
        }

        @Test
        @DisplayName("Should update price to zero")
        void shouldUpdatePriceToZero() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setPrice(0f);

            // Act
            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            // Assert
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getPrice()));
        }

        @Test
        @DisplayName("Should handle empty update request (no changes)")
        void shouldHandleEmptyUpdateRequest() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();

            // Act
            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            // Assert - No changes should be made
            assertEquals("Original Title", result.getTitle());
            assertEquals("Original Description", result.getDescription());
            assertEquals(0, BigDecimal.valueOf(50).compareTo(result.getPrice()));
        }

        @Test
        @DisplayName("Should update condition to each valid value")
        void shouldUpdateConditionToValidValues() {
            for (CreateItemRequestCondition condition : CreateItemRequestCondition.values()) {
                // Arrange
                UpdateItemRequest request = new UpdateItemRequest();
                request.setCondition(condition);

                // Act
                MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

                // Assert
                assertEquals(condition.getValue(), result.getCondition());
            }
        }

        @Test
        @DisplayName("Should trim title whitespace on update")
        void shouldTrimTitleWhitespaceOnUpdate() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("  Trimmed Update  ");

            // Act
            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            // Assert
            assertEquals("Trimmed Update", result.getTitle());
        }
    }

    @Nested
    @DisplayName("Update Item - Negative Cases")
    class UpdateItemNegativeTests {

        @Test
        @DisplayName("Should fail when item not found")
        void shouldFailWhenItemNotFound() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("New Title");
            UUID nonExistentId = UUID.randomUUID();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                marketplaceService.updateItem(nonExistentId, request, testUser.getId());
            });
            assertTrue(exception.getMessage().contains("Item not found"));
        }

        @Test
        @DisplayName("Should fail when user is not the owner")
        void shouldFailWhenNotOwner() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("Hacked Title");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                marketplaceService.updateItem(testItem.getId(), request, otherUser.getId());
            });
            assertTrue(exception.getMessage().contains("You can only update your own items"));
        }

        @Test
        @DisplayName("Should fail when title is empty")
        void shouldFailWhenTitleIsEmpty() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("   ");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                marketplaceService.updateItem(testItem.getId(), request, testUser.getId());
            });
            assertTrue(exception.getMessage().contains("Title cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when price is negative")
        void shouldFailWhenPriceIsNegative() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setPrice(-10f);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                marketplaceService.updateItem(testItem.getId(), request, testUser.getId());
            });
            assertTrue(exception.getMessage().contains("Price must be greater than or equal to 0"));
        }

        @Test
        @DisplayName("Should fail when title exceeds 255 characters")
        void shouldFailWhenTitleTooLong() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("a".repeat(256));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                marketplaceService.updateItem(testItem.getId(), request, testUser.getId());
            });
            assertTrue(exception.getMessage().contains("Title cannot exceed 255 characters"));
        }
    }

    @Nested
    @DisplayName("Update Item - Boundary Cases")
    class UpdateItemBoundaryTests {

        @Test
        @DisplayName("Should update title to exactly 255 characters")
        void shouldUpdateTitleToMaxLength() {
            // Arrange
            String maxTitle = "b".repeat(255);
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle(maxTitle);

            // Act
            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            // Assert
            assertEquals(255, result.getTitle().length());
            assertEquals(maxTitle, result.getTitle());
        }

        @Test
        @DisplayName("Should update title to one character")
        void shouldUpdateTitleToOneChar() {
            // Arrange
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("X");

            // Act
            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            // Assert
            assertEquals("X", result.getTitle());
        }

        @Test
        @DisplayName("Should update with very large price")
        void shouldUpdateWithLargePrice() {
            // Arrange - Use a value well within DECIMAL(10,2) limits (max: 99999999.99)
            UpdateItemRequest request = new UpdateItemRequest();
            request.setPrice(9999999.99f);

            // Act
            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            // Assert
            assertTrue(result.getPrice().compareTo(BigDecimal.valueOf(9999999.0)) > 0);
        }
    }
}
