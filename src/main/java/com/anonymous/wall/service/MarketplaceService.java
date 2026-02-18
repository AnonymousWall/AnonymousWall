package com.anonymous.wall.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.model.UpdateItemRequest;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

public interface MarketplaceService {

    /**
     * Create a new marketplace item
     *
     * @param request the item creation request
     * @param userId  the user creating the item
     * @return the created marketplace item
     */
    MarketplaceItem createItem(CreateItemRequest request, UUID userId);

    /**
     * Update an existing marketplace item
     *
     * @param itemId  the ID of the item to update
     * @param request the update request with optional fields
     * @param userId  the user attempting the update
     * @return the updated marketplace item
     */
    MarketplaceItem updateItem(UUID itemId, UpdateItemRequest request, UUID userId);

    /**
     * List marketplace items with pagination and sorting
     *
     * @param pageable pagination and sorting parameters
     * @param sortBy   sort option (newest, price-asc, price-desc)
     * @param sold     optional filter by sold status (null for all items)
     * @return page of marketplace items
     */
    Page<MarketplaceItem> listItems(Pageable pageable, String sortBy, Boolean sold);

    /**
     * Get a specific marketplace item by ID
     *
     * @param itemId the item ID
     * @return the marketplace item
     */
    MarketplaceItem getItem(UUID itemId);
}
