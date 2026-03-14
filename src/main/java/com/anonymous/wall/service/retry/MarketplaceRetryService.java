package com.anonymous.wall.service.retry;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.model.UpdateItemRequest;
import com.anonymous.wall.service.base.MarketplaceService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;

/**
 * Marketplace retry wrapper.
 */
@Singleton
public class MarketplaceRetryService {

    private final MarketplaceService marketplaceService;

    public MarketplaceRetryService(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public MarketplaceItem createItem(CreateItemRequest request, List<CompletedFileUpload> images, UUID userId) {
        return marketplaceService.createItem(request, images, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public MarketplaceItem updateItem(UUID itemId, UpdateItemRequest request, UUID userId) {
        return marketplaceService.updateItem(itemId, request, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Page<MarketplaceItem> listItems(Pageable pageable, String sortBy) {
        return marketplaceService.listItems(pageable, sortBy);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Page<MarketplaceItem> getItemsByWall(String wall, Pageable pageable, UUID userId, String schoolDomain, String sortBy, String category) {
        return marketplaceService.getItemsByWall(wall, pageable, userId, schoolDomain, sortBy, category);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public MarketplaceItem getItem(UUID itemId) {
        return marketplaceService.getItem(itemId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public MarketplaceItem getItem(UUID itemId, UUID userId) {
        return marketplaceService.getItem(itemId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public Page<MarketplaceItem> getUserOwnItems(UUID userId, Pageable pageable, String sortBy) {
        return marketplaceService.getUserOwnItems(userId, pageable, sortBy);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public void hideItem(UUID itemId, UUID userId) {
        marketplaceService.hideItem(itemId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms", excludes = IllegalArgumentException.class)
    public void unhideItem(UUID itemId, UUID userId) {
        marketplaceService.unhideItem(itemId, userId);
    }
}