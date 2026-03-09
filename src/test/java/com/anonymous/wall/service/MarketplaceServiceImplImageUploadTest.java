package com.anonymous.wall.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.impl.MarketplaceServiceImpl;
import com.anonymous.wall.util.MediaUtilInterface;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("MarketplaceServiceImpl - Image Upload Tests")
class MarketplaceServiceImplImageUploadTest {

    private MarketplaceServiceImpl marketplaceService;
    private MarketplaceItemRepository marketplaceItemRepository;
    private UserRepository userRepository;
    private MediaUtilInterface mediaUtil;

    @BeforeEach
    void setUp() {
        marketplaceItemRepository = mock(MarketplaceItemRepository.class);
        userRepository = mock(UserRepository.class);
        mediaUtil = mock(MediaUtilInterface.class);

        marketplaceService = new MarketplaceServiceImpl();

        try {
            setField("marketplaceItemRepository", marketplaceItemRepository);
            setField("userRepository", userRepository);
            setField("mediaUtil", mediaUtil);
            setField("commentRepository", mock(com.anonymous.wall.repository.CommentRepository.class));
            setField("marketplaceItemHiddenEventPublisher", mock(ApplicationEventPublisher.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(String name, Object value) throws Exception {
        var field = MarketplaceServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(marketplaceService, value);
    }

    private UserEntity createUser(String schoolDomain) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("student@" + schoolDomain);
        user.setSchoolDomain(schoolDomain);
        user.setProfileName("TestSeller");
        return user;
    }

    private MarketplaceItem createSavedItem(UUID userId, String title, BigDecimal price) {
        MarketplaceItem item = new MarketplaceItem();
        item.setId(UUID.randomUUID());
        item.setUserId(userId);
        item.setTitle(title);
        item.setPrice(price);
        item.setWall("campus");
        item.setSchoolDomain("test.edu");
        return item;
    }

    @Nested
    @DisplayName("createItem with images")
    class CreateItemWithImageTests {

        @Test
        @DisplayName("Should create item with single image when provided")
        void shouldCreateItemWithSingleImage() {
            UserEntity user = createUser("test.edu");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            String imageUrl = "http://localhost:8080/media/marketplace/test.jpg";
            when(mediaUtil.uploadMarketplaceImage(any(), any())).thenReturn(imageUrl);

            MarketplaceItem savedItem = createSavedItem(user.getId(), "Test Item", BigDecimal.valueOf(50.0));
            savedItem.setImageUrls(List.of(imageUrl));
            when(marketplaceItemRepository.save(any())).thenReturn(savedItem);

            CompletedFileUpload image = mock(CompletedFileUpload.class);
            when(image.getSize()).thenReturn(1024L);
            when(image.getContentType()).thenReturn(Optional.of(MediaType.IMAGE_JPEG_TYPE));

            CreateItemRequest request = new CreateItemRequest("Test Item", 50.0f);
            MarketplaceItem result = marketplaceService.createItem(request, List.of(image), user.getId());

            assertNotNull(result);
            assertEquals(List.of(imageUrl), result.getImageUrls());
            verify(mediaUtil, times(1)).uploadMarketplaceImage(image, user.getId());
        }

        @Test
        @DisplayName("Should create item with multiple images")
        void shouldCreateItemWithMultipleImages() {
            UserEntity user = createUser("test.edu");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            String url1 = "http://localhost:8080/media/marketplace/img1.jpg";
            String url2 = "http://localhost:8080/media/marketplace/img2.png";
            when(mediaUtil.uploadMarketplaceImage(any(), any())).thenReturn(url1, url2);

            MarketplaceItem savedItem = createSavedItem(user.getId(), "Test", BigDecimal.valueOf(10.0));
            savedItem.setImageUrls(List.of(url1, url2));
            when(marketplaceItemRepository.save(any())).thenReturn(savedItem);

            CompletedFileUpload img1 = mock(CompletedFileUpload.class);
            when(img1.getSize()).thenReturn(1024L);
            CompletedFileUpload img2 = mock(CompletedFileUpload.class);
            when(img2.getSize()).thenReturn(2048L);

            CreateItemRequest request = new CreateItemRequest("Test", 10.0f);
            MarketplaceItem result = marketplaceService.createItem(request, List.of(img1, img2), user.getId());

            assertNotNull(result);
            assertEquals(2, result.getImageUrls().size());
            verify(mediaUtil, times(2)).uploadMarketplaceImage(any(), any());
        }

        @Test
        @DisplayName("Should fail when more than 5 images are provided")
        void shouldFailWhenTooManyImages() {
            UserEntity user = createUser("test.edu");
            CreateItemRequest request = new CreateItemRequest("Title", 10.0f);

            List<CompletedFileUpload> images = List.of(
                mock(CompletedFileUpload.class), mock(CompletedFileUpload.class),
                mock(CompletedFileUpload.class), mock(CompletedFileUpload.class),
                mock(CompletedFileUpload.class), mock(CompletedFileUpload.class)
            );

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> marketplaceService.createItem(request, images, user.getId()));
            assertTrue(ex.getMessage().contains("5"));
        }

        @Test
        @DisplayName("Should create item without images when list is null")
        void shouldCreateItemWithoutImagesWhenNull() {
            UserEntity user = createUser("test.edu");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            MarketplaceItem savedItem = createSavedItem(user.getId(), "Test", BigDecimal.valueOf(10.0));
            when(marketplaceItemRepository.save(any())).thenReturn(savedItem);

            CreateItemRequest request = new CreateItemRequest("Test", 10.0f);
            MarketplaceItem result = marketplaceService.createItem(request, null, user.getId());

            assertNotNull(result);
            assertTrue(result.getImageUrls().isEmpty());
            verify(mediaUtil, never()).uploadMarketplaceImage(any(), any());
        }

        @Test
        @DisplayName("Should create item without images when list is empty")
        void shouldCreateItemWithoutImagesWhenEmpty() {
            UserEntity user = createUser("test.edu");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            MarketplaceItem savedItem = createSavedItem(user.getId(), "Test", BigDecimal.valueOf(10.0));
            when(marketplaceItemRepository.save(any())).thenReturn(savedItem);

            CreateItemRequest request = new CreateItemRequest("Test", 10.0f);
            MarketplaceItem result = marketplaceService.createItem(request, List.of(), user.getId());

            assertNotNull(result);
            assertTrue(result.getImageUrls().isEmpty());
            verify(mediaUtil, never()).uploadMarketplaceImage(any(), any());
        }

        @Test
        @DisplayName("Should skip images with zero size")
        void shouldSkipZeroSizeImages() {
            UserEntity user = createUser("test.edu");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            MarketplaceItem savedItem = createSavedItem(user.getId(), "Test", BigDecimal.valueOf(10.0));
            when(marketplaceItemRepository.save(any())).thenReturn(savedItem);

            CompletedFileUpload emptyImage = mock(CompletedFileUpload.class);
            when(emptyImage.getSize()).thenReturn(0L);

            CreateItemRequest request = new CreateItemRequest("Test", 10.0f);
            MarketplaceItem result = marketplaceService.createItem(request, List.of(emptyImage), user.getId());

            assertNotNull(result);
            verify(mediaUtil, never()).uploadMarketplaceImage(any(), any());
        }

        @Test
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());
            CreateItemRequest request = new CreateItemRequest("Title", 10.0f);

            assertThrows(IllegalArgumentException.class,
                () -> marketplaceService.createItem(request, null, UUID.randomUUID()));
        }

        @Test
        @DisplayName("Should propagate exception from media upload")
        void shouldPropagateMediaUploadException() {
            UserEntity user = createUser("test.edu");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(mediaUtil.uploadMarketplaceImage(any(), any()))
                .thenThrow(new IllegalArgumentException("Image exceeds 5MB limit"));

            CompletedFileUpload image = mock(CompletedFileUpload.class);
            when(image.getSize()).thenReturn(6 * 1024 * 1024L);
            when(image.getContentType()).thenReturn(Optional.of(MediaType.IMAGE_JPEG_TYPE));

            CreateItemRequest request = new CreateItemRequest("Title", 10.0f);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> marketplaceService.createItem(request, List.of(image), user.getId()));
            assertTrue(ex.getMessage().contains("5MB"));
        }
    }
}
