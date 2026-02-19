package com.anonymous.wall.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.model.UpdateItemRequest;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

public interface MarketplaceService {

    MarketplaceItem createItem(CreateItemRequest request, UUID userId);

    MarketplaceItem updateItem(UUID itemId, UpdateItemRequest request, UUID userId);

    Page<MarketplaceItem> listItems(Pageable pageable, String sortBy, Boolean sold);

    Page<MarketplaceItem> getItemsByWall(String wall, Pageable pageable, UUID userId, String schoolDomain, String sortBy, Boolean sold);

    MarketplaceItem getItem(UUID itemId);

    MarketplaceItem getItem(UUID itemId, UUID userId);

    Page<MarketplaceItem> getUserOwnItems(UUID userId, Pageable pageable, String sortBy);
}
