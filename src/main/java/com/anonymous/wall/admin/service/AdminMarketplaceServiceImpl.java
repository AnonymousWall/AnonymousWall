package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Singleton
public class AdminMarketplaceServiceImpl implements AdminMarketplaceService {

    private static final Logger log = LoggerFactory.getLogger(AdminMarketplaceServiceImpl.class);

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;

    @Override
    public Page<MarketplaceItem> getAllMarketplaces(Pageable pageable, UUID userId, Boolean hidden, String sortBy, String sortOrder) {
        log.info("Admin fetching marketplaces - userId={}, hidden={}, sortBy={}, sortOrder={}", userId, hidden, sortBy, sortOrder);

        boolean isDesc = sortOrder == null || sortOrder.equalsIgnoreCase("desc");

        if (userId == null && hidden == null) {
            if (sortBy == null) {
                return marketplaceItemRepository.findAll(pageable);
            }
            return isDesc
                    ? marketplaceItemRepository.findAllOrderByCreatedAtDesc(pageable)
                    : marketplaceItemRepository.findAllOrderByCreatedAtAsc(pageable);
        }

        if (userId != null && hidden == null) {
            return marketplaceItemRepository.findByUserId(userId, pageable);
        }
        if (userId == null && hidden != null) {
            return marketplaceItemRepository.findByHidden(hidden, pageable);
        }
        return marketplaceItemRepository.findByUserIdAndHidden(userId, hidden, pageable);
    }

    @Override
    public MarketplaceItem getMarketplaceById(UUID id) {
        log.info("Admin fetching marketplace item by id: {}", id);
        return marketplaceItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marketplace item not found with ID: " + id));
    }

    @Override
    public void hideMarketplace(UUID id) {
        log.info("Admin hiding marketplace item: {}", id);
        MarketplaceItem item = marketplaceItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marketplace item not found with ID: " + id));
        item.setHidden(true);
        marketplaceItemRepository.update(item);
    }

    @Override
    public void unhideMarketplace(UUID id) {
        log.info("Admin unhiding marketplace item: {}", id);
        MarketplaceItem item = marketplaceItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marketplace item not found with ID: " + id));
        item.setHidden(false);
        marketplaceItemRepository.update(item);
    }

    @Override
    public Page<MarketplaceItem> getMarketplacesByUserId(UUID userId, Pageable pageable) {
        log.info("Admin fetching marketplaces for user: {}", userId);
        return marketplaceItemRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public List<String> getMarketplaceImages(UUID id) {
        log.info("Admin fetching images for marketplace item: {}", id);
        MarketplaceItem item = marketplaceItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marketplace item not found with ID: " + id));
        return item.getImageUrls();
    }
}
