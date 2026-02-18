package com.anonymous.wall.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.model.UpdateItemRequest;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class MarketplaceServiceImpl implements MarketplaceService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceServiceImpl.class);

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private UserRepository userRepository;

    @Override
    @Transactional
    public MarketplaceItem createItem(CreateItemRequest request, UUID userId) {
        log.info("Creating marketplace item for user {}", userId);

        // Validate user exists
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        // Validate required fields
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (request.getTitle().length() > 255) {
            throw new IllegalArgumentException("Title cannot exceed 255 characters");
        }

        if (request.getPrice() == null) {
            throw new IllegalArgumentException("Price is required");
        }

        BigDecimal price = BigDecimal.valueOf(request.getPrice());
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be greater than or equal to 0");
        }

        // Validate condition if provided
        if (request.getCondition() != null) {
            String condition = request.getCondition().getValue();
            if (!isValidCondition(condition)) {
                throw new IllegalArgumentException("Invalid condition. Must be one of: new, like-new, good, fair");
            }
        }

        // Create the item
        MarketplaceItem item = new MarketplaceItem();
        item.setUserId(userId);
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription() != null ? request.getDescription() : null);
        item.setPrice(price);
        item.setCategory(request.getCategory() != null ? request.getCategory() : null);
        item.setCondition(request.getCondition() != null ? request.getCondition().getValue() : null);
        item.setSold(false);
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());

        MarketplaceItem saved = marketplaceItemRepository.save(item);
        log.info("Created marketplace item {} for user {}", saved.getId(), userId);
        return saved;
    }

    @Override
    @Transactional
    public MarketplaceItem updateItem(UUID itemId, UpdateItemRequest request, UUID userId) {
        log.info("Updating marketplace item {} for user {}", itemId, userId);

        // Find the item
        Optional<MarketplaceItem> itemOpt = marketplaceItemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            throw new IllegalArgumentException("Item not found");
        }

        MarketplaceItem item = itemOpt.get();

        // Check ownership
        if (!item.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only update your own items");
        }

        // Perform null-safe partial updates
        boolean updated = false;

        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("Title cannot be empty");
            }
            if (title.length() > 255) {
                throw new IllegalArgumentException("Title cannot exceed 255 characters");
            }
            item.setTitle(title);
            updated = true;
        }

        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
            updated = true;
        }

        if (request.getPrice() != null) {
            BigDecimal price = BigDecimal.valueOf(request.getPrice());
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price must be greater than or equal to 0");
            }
            item.setPrice(price);
            updated = true;
        }

        if (request.getCategory() != null) {
            item.setCategory(request.getCategory());
            updated = true;
        }

        if (request.getCondition() != null) {
            String condition = request.getCondition().getValue();
            if (!isValidCondition(condition)) {
                throw new IllegalArgumentException("Invalid condition. Must be one of: new, like-new, good, fair");
            }
            item.setCondition(condition);
            updated = true;
        }

        if (request.getSold() != null) {
            item.setSold(request.getSold());
            updated = true;
        }

        if (updated) {
            item.setUpdatedAt(OffsetDateTime.now());
            MarketplaceItem saved = marketplaceItemRepository.update(item);
            log.info("Updated marketplace item {}", itemId);
            return saved;
        }

        log.info("No changes made to marketplace item {}", itemId);
        return item;
    }

    @Override
    public Page<MarketplaceItem> listItems(Pageable pageable, String sortBy) {
        log.info("Listing marketplace items with sortBy={}", sortBy);

        if (sortBy == null) {
            sortBy = "newest";
        }

        switch (sortBy.toLowerCase()) {
            case "price-asc":
                return marketplaceItemRepository.findAllOrderByPriceAsc(pageable);
            case "price-desc":
                return marketplaceItemRepository.findAllOrderByPriceDesc(pageable);
            case "newest":
            default:
                return marketplaceItemRepository.findAllOrderByCreatedAtDesc(pageable);
        }
    }

    @Override
    public MarketplaceItem getItem(UUID itemId) {
        log.info("Getting marketplace item {}", itemId);
        return marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
    }

    private boolean isValidCondition(String condition) {
        if (condition == null) {
            return false;
        }
        return condition.equals("new") || condition.equals("like-new") || 
               condition.equals("good") || condition.equals("fair");
    }
}
