package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.model.UpdateItemRequest;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.multipart.CompletedFileUpload;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketplaceService {

    MarketplaceItem createItem(CreateItemRequest request, List<CompletedFileUpload> images, UUID userId);

    MarketplaceItem updateItem(UUID itemId, UpdateItemRequest request, UUID userId);

    Page<MarketplaceItem> listItems(Pageable pageable, String sortBy);

    Page<MarketplaceItem> getItemsByWall(String wall, Pageable pageable, UUID userId, String schoolDomain, String sortBy, String category);

    MarketplaceItem getItem(UUID itemId);

    MarketplaceItem getItem(UUID itemId, UUID userId);

    Page<MarketplaceItem> getUserOwnItems(UUID userId, Pageable pageable, String sortBy);

    void hideItem(UUID itemId, UUID userId);

    void unhideItem(UUID itemId, UUID userId);

    void updateProfileNameByUserId(UUID userId, String profileName);

    Optional<MarketplaceItem> findById(UUID itemId);

    void update(MarketplaceItem item);
}
