package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import io.micronaut.cache.SyncCache;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class MarketplaceCache {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceCache.class);

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    @Named("national-marketplace")
    private SyncCache<Object> cache;

    /**
     * schoolDomain=null → national wall
     * schoolDomain="harvard.edu" → campus wall for that school
     */
    public Page<MarketplaceItem> get(int page, int size, String sortBy, String category, String schoolDomain) {
        String key = page + "_" + size + "_" + sortBy.toLowerCase()
                + "_" + (category != null ? category : "null")
                + "_" + (schoolDomain != null ? schoolDomain : "national");
        try {
            Optional<Page> cached = cache.get(key, Page.class);
            if (cached.isPresent()) {
                log.debug("Cache hit for marketplace: key={}", key);
                //noinspection unchecked
                return (Page<MarketplaceItem>) cached.get();
            }
        } catch (Exception e) {
            log.warn("Cache get failed for key={}, falling through to DB: {}", key, e.getMessage());
        }

        log.debug("Cache miss for marketplace: key={}", key);
        Page<MarketplaceItem> result = fetchFromDb(page, size, sortBy, category, schoolDomain);

        try {
            cache.put(key, result);
        } catch (Exception e) {
            log.warn("Cache put failed for key={}: {}", key, e.getMessage());
        }

        return result;
    }

    public void invalidateAll() {
        log.debug("Invalidating marketplace cache");
        cache.invalidateAll();
    }

    private Page<MarketplaceItem> fetchFromDb(int page, int size, String sortBy, String category, String schoolDomain) {
        Pageable pageable = Pageable.from(page, size);
        if (schoolDomain != null) {
            // Campus wall
            if (category != null) {
                return switch (sortBy.toLowerCase()) {
                    case "oldest"     -> marketplaceItemRepository.findByWallAndSchoolDomainAndCategoryAndHiddenFalseOrderByCreatedAtAsc("campus", schoolDomain, category, pageable);
                    case "price-asc"  -> marketplaceItemRepository.findByWallAndSchoolDomainAndCategoryAndHiddenFalseOrderByPriceAsc("campus", schoolDomain, category, pageable);
                    case "price-desc" -> marketplaceItemRepository.findByWallAndSchoolDomainAndCategoryAndHiddenFalseOrderByPriceDesc("campus", schoolDomain, category, pageable);
                    default           -> marketplaceItemRepository.findByWallAndSchoolDomainAndCategoryAndHiddenFalseOrderByCreatedAtDesc("campus", schoolDomain, category, pageable);
                };
            } else {
                return switch (sortBy.toLowerCase()) {
                    case "oldest"     -> marketplaceItemRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtAsc("campus", schoolDomain, pageable);
                    case "price-asc"  -> marketplaceItemRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByPriceAsc("campus", schoolDomain, pageable);
                    case "price-desc" -> marketplaceItemRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByPriceDesc("campus", schoolDomain, pageable);
                    default           -> marketplaceItemRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc("campus", schoolDomain, pageable);
                };
            }
        } else {
            // National wall
            if (category != null) {
                return switch (sortBy.toLowerCase()) {
                    case "oldest"     -> marketplaceItemRepository.findByWallAndCategoryAndHiddenFalseOrderByCreatedAtAsc("national", category, pageable);
                    case "price-asc"  -> marketplaceItemRepository.findByWallAndCategoryAndHiddenFalseOrderByPriceAsc("national", category, pageable);
                    case "price-desc" -> marketplaceItemRepository.findByWallAndCategoryAndHiddenFalseOrderByPriceDesc("national", category, pageable);
                    default           -> marketplaceItemRepository.findByWallAndCategoryAndHiddenFalseOrderByCreatedAtDesc("national", category, pageable);
                };
            } else {
                return switch (sortBy.toLowerCase()) {
                    case "oldest"     -> marketplaceItemRepository.findByWallAndHiddenFalseOrderByCreatedAtAsc("national", pageable);
                    case "price-asc"  -> marketplaceItemRepository.findByWallAndHiddenFalseOrderByPriceAsc("national", pageable);
                    case "price-desc" -> marketplaceItemRepository.findByWallAndHiddenFalseOrderByPriceDesc("national", pageable);
                    default           -> marketplaceItemRepository.findByWallAndHiddenFalseOrderByCreatedAtDesc("national", pageable);
                };
            }
        }
    }
}