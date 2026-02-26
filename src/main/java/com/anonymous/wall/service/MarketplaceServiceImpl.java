package com.anonymous.wall.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.event.MarketplaceItemHiddenEvent;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.model.UpdateItemRequest;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.util.MediaUtilInterface;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class MarketplaceServiceImpl implements MarketplaceService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceServiceImpl.class);
    private static final int MAX_IMAGES_PER_ITEM = 5;

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private ApplicationEventPublisher<MarketplaceItemHiddenEvent> marketplaceItemHiddenEventPublisher;

    @Inject
    private MediaUtilInterface mediaUtil;

    @Override
    @Transactional
    public MarketplaceItem createItem(CreateItemRequest request, List<CompletedFileUpload> images, UUID userId) {
        log.info("Creating marketplace item for user {}", userId);

        // Validate image count before any DB access
        if (images != null && images.size() > MAX_IMAGES_PER_ITEM) {
            throw new IllegalArgumentException("Maximum " + MAX_IMAGES_PER_ITEM + " images per item allowed");
        }

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        UserEntity user = userOpt.get();

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        String trimmedTitle = request.getTitle().trim();

        if (trimmedTitle.length() > 255) {
            throw new IllegalArgumentException("Title cannot exceed 255 characters");
        }

        if (request.getPrice() == null) {
            throw new IllegalArgumentException("Price is required");
        }

        BigDecimal price = BigDecimal.valueOf(request.getPrice());
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be greater than or equal to 0");
        }

        if (request.getCondition() != null) {
            String condition = request.getCondition().getValue();
            if (!isValidCondition(condition)) {
                throw new IllegalArgumentException("Invalid condition. Must be one of: new, like-new, good, fair");
            }
        }

        // Determine wall: if wall is not specified in request, default to campus
        String wall = "campus";
        if (request.getWall() != null) {
            String wallStr = request.getWall().getValue();
            if ("national".equals(wallStr) || "campus".equals(wallStr)) {
                wall = wallStr;
            }
        }

        // For campus posts, validate school domain
        String schoolDomain = null;
        if ("campus".equals(wall)) {
            schoolDomain = user.getSchoolDomain();
            if (schoolDomain == null || schoolDomain.trim().isEmpty()) {
                throw new IllegalArgumentException("You must have a school domain to post to campus wall");
            }
        }

        MarketplaceItem item = new MarketplaceItem();
        item.setUserId(userId);
        item.setProfileName(user.getProfileName());
        item.setTitle(trimmedTitle);
        item.setDescription(request.getDescription() != null ? request.getDescription() : null);
        item.setPrice(price);
        item.setCategory(request.getCategory() != null ? request.getCategory().getValue() : null);
        item.setCondition(request.getCondition() != null ? request.getCondition().getValue() : null);
        item.setWall(wall);
        item.setSchoolDomain(schoolDomain);
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());

        // Upload images
        List<String> imageUrls = new ArrayList<>();
        if (images != null) {
            for (CompletedFileUpload image : images) {
                if (image != null && image.getSize() > 0) {
                    imageUrls.add(mediaUtil.uploadMarketplaceImage(image, userId));
                }
            }
        }
        item.setImageUrls(imageUrls);

        MarketplaceItem saved = marketplaceItemRepository.save(item);
        log.info("Created marketplace item {} for user {}, wall={}, schoolDomain={}, imageCount={}", saved.getId(), userId, wall, schoolDomain, imageUrls.size());
        return saved;
    }

    @Override
    @Transactional
    public MarketplaceItem updateItem(UUID itemId, UpdateItemRequest request, UUID userId) {
        log.info("Updating marketplace item {} for user {}", itemId, userId);

        Optional<MarketplaceItem> itemOpt = marketplaceItemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            throw new IllegalArgumentException("Item not found");
        }

        MarketplaceItem item = itemOpt.get();

        if (!item.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only update your own items");
        }

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
            item.setCategory(request.getCategory().getValue());
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
    public Page<MarketplaceItem> getItemsByWall(String wall, Pageable pageable, UUID userId, String schoolDomain, String sortBy, String category) {
        log.info("Listing marketplace items by wall={}, sortBy={}, schoolDomain={}, category={}", wall, sortBy, schoolDomain, category);

        if (sortBy == null) {
            sortBy = "newest";
        }

        if ("campus".equals(wall)) {
            if (schoolDomain == null || schoolDomain.trim().isEmpty()) {
                throw new IllegalArgumentException("School domain is required to view campus marketplace items");
            }
            if (category != null) {
                switch (sortBy.toLowerCase()) {
                    case "oldest":
                        return marketplaceItemRepository.findByWallAndSchoolDomainAndCategoryAndHiddenFalseOrderByCreatedAtAsc("campus", schoolDomain, category, pageable);
                    case "price-asc":
                        return marketplaceItemRepository.findByWallAndSchoolDomainAndCategoryAndHiddenFalseOrderByPriceAsc("campus", schoolDomain, category, pageable);
                    case "price-desc":
                        return marketplaceItemRepository.findByWallAndSchoolDomainAndCategoryAndHiddenFalseOrderByPriceDesc("campus", schoolDomain, category, pageable);
                    case "newest":
                    default:
                        return marketplaceItemRepository.findByWallAndSchoolDomainAndCategoryAndHiddenFalseOrderByCreatedAtDesc("campus", schoolDomain, category, pageable);
                }
            } else {
                switch (sortBy.toLowerCase()) {
                    case "oldest":
                        return marketplaceItemRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtAsc("campus", schoolDomain, pageable);
                    case "price-asc":
                        return marketplaceItemRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByPriceAsc("campus", schoolDomain, pageable);
                    case "price-desc":
                        return marketplaceItemRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByPriceDesc("campus", schoolDomain, pageable);
                    case "newest":
                    default:
                        return marketplaceItemRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc("campus", schoolDomain, pageable);
                }
            }
        } else {
            // National wall
            if (category != null) {
                switch (sortBy.toLowerCase()) {
                    case "oldest":
                        return marketplaceItemRepository.findByWallAndCategoryAndHiddenFalseOrderByCreatedAtAsc("national", category, pageable);
                    case "price-asc":
                        return marketplaceItemRepository.findByWallAndCategoryAndHiddenFalseOrderByPriceAsc("national", category, pageable);
                    case "price-desc":
                        return marketplaceItemRepository.findByWallAndCategoryAndHiddenFalseOrderByPriceDesc("national", category, pageable);
                    case "newest":
                    default:
                        return marketplaceItemRepository.findByWallAndCategoryAndHiddenFalseOrderByCreatedAtDesc("national", category, pageable);
                }
            } else {
                switch (sortBy.toLowerCase()) {
                    case "oldest":
                        return marketplaceItemRepository.findByWallAndHiddenFalseOrderByCreatedAtAsc("national", pageable);
                    case "price-asc":
                        return marketplaceItemRepository.findByWallAndHiddenFalseOrderByPriceAsc("national", pageable);
                    case "price-desc":
                        return marketplaceItemRepository.findByWallAndHiddenFalseOrderByPriceDesc("national", pageable);
                    case "newest":
                    default:
                        return marketplaceItemRepository.findByWallAndHiddenFalseOrderByCreatedAtDesc("national", pageable);
                }
            }
        }
    }

    @Override
    public MarketplaceItem getItem(UUID itemId) {
        log.info("Getting marketplace item {}", itemId);
        return marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
    }

    @Override
    public MarketplaceItem getItem(UUID itemId, UUID userId) {
        log.info("Getting marketplace item {} for user {}", itemId, userId);
        MarketplaceItem item = marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        if (item.isHidden()) {
            throw new IllegalArgumentException("Item not found");
        }

        if ("campus".equals(item.getWall())) {
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found");
            }
            UserEntity user = userOpt.get();
            String userSchoolDomain = user.getSchoolDomain();
            if (userSchoolDomain == null || !userSchoolDomain.equals(item.getSchoolDomain())) {
                throw new IllegalArgumentException("You do not have access to posts from other schools");
            }
        }

        return item;
    }

    @Override
    public Page<MarketplaceItem> getUserOwnItems(UUID userId, Pageable pageable, String sortBy) {
        log.info("Getting own marketplace items for user {}, sortBy={}", userId, sortBy);

        if (sortBy == null) {
            sortBy = "newest";
        }

        switch (sortBy.toLowerCase()) {
            case "oldest":
                return marketplaceItemRepository.findByUserIdAndHiddenFalseOrderByCreatedAtAsc(userId, pageable);
            case "newest":
            default:
                return marketplaceItemRepository.findByUserIdAndHiddenFalseOrderByCreatedAtDesc(userId, pageable);
        }
    }

    private boolean isValidCondition(String condition) {
        if (condition == null) {
            return false;
        }
        return condition.equals("new") || condition.equals("like-new") || 
               condition.equals("good") || condition.equals("fair") || condition.equals("poor");
    }

    @Override
    @Transactional
    public void hideItem(UUID itemId, UUID userId) {
        log.info("Hiding marketplace item {} for user {}", itemId, userId);
        MarketplaceItem item = marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        if (!item.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only hide your own items");
        }
        item.setHidden(true);
        item.setUpdatedAt(java.time.OffsetDateTime.now());
        marketplaceItemRepository.update(item);
        // Publish event for async comment hiding
        marketplaceItemHiddenEventPublisher.publishEvent(new MarketplaceItemHiddenEvent(itemId, userId));
        log.debug("Published MarketplaceItemHiddenEvent for itemId={}", itemId);
        log.info("Hid marketplace item {}", itemId);
    }

    @Override
    @Transactional
    public void unhideItem(UUID itemId, UUID userId) {
        log.info("Unhiding marketplace item {} for user {}", itemId, userId);
        MarketplaceItem item = marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        if (!item.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only unhide your own items");
        }
        item.setHidden(false);
        item.setUpdatedAt(java.time.OffsetDateTime.now());
        marketplaceItemRepository.update(item);
        // Unhide all comments associated with this item (within same transaction)
        commentRepository.updateByParentTypeAndParentId("MARKETPLACE", itemId, false);
        log.info("Unhid marketplace item {}", itemId);
    }
}
