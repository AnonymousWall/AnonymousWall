package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.MarketplaceItem;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdminMarketplaceService {
    Page<MarketplaceItem> getAllMarketplaces(Pageable pageable, UUID userId, Boolean hidden, String sortBy, String sortOrder);
    MarketplaceItem getMarketplaceById(UUID id);
    void hideMarketplace(UUID id);
    void unhideMarketplace(UUID id);
    Page<MarketplaceItem> getMarketplacesByUserId(UUID userId, Pageable pageable);
    List<String> getMarketplaceImages(UUID id);
}
