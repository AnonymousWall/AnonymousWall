package com.anonymous.wall.controller;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.model.ItemDTO;
import com.anonymous.wall.model.UpdateItemRequest;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.MarketplaceService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller("/api/v1/marketplace")
public class MarketplaceController {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceController.class);

    @Inject
    private MarketplaceService marketplaceService;

    @Inject
    private UserRepository userRepository;

    // Helper to extract user ID from Principal
    private UUID getUserIdFromRequest(HttpRequest<?> request) {
        Optional<Principal> principalOpt = request.getUserPrincipal();

        if (principalOpt.isEmpty()) {
            throw new IllegalArgumentException("User not authenticated");
        }

        String principalName = principalOpt.get().getName();
        try {
            return UUID.fromString(principalName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user ID format in security context: " + principalName, e);
        }
    }

    /**
     * POST /marketplace
     * Create a new marketplace item
     */
    @Post
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> createItem(@Body CreateItemRequest request, HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("POST /marketplace - Creating new item, user={}, title={}", userId, request.getTitle());

            MarketplaceItem item = marketplaceService.createItem(request, userId);
            ItemDTO dto = mapItemToDTO(item);

            log.info("POST /marketplace - Item created successfully, itemId={}", dto.getId());
            return HttpResponse.created(dto);
        } catch (IllegalArgumentException e) {
            log.warn("POST /marketplace - Bad request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /marketplace - Error creating item", e);
            return HttpResponse.badRequest(error("Failed to create marketplace item"));
        }
    }

    /**
     * GET /marketplace
     * List marketplace items with optional pagination, sorting, and filtering
     */
    @Get
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> listItems(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @QueryValue(defaultValue = "newest") String sortBy,
            @Nullable @QueryValue Boolean sold,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("GET /marketplace - Listing items, user={}, page={}, limit={}, sortBy={}, sold={}", 
                    userId, page, limit, sortBy, sold);

            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            Pageable pageable = Pageable.from(page - 1, limit);
            Page<MarketplaceItem> items = marketplaceService.listItems(pageable, sortBy, sold);

            List<ItemDTO> dtos = items.getContent().stream()
                    .map(this::mapItemToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", dtos);
            response.put("pagination", createPaginationInfo(items));

            log.info("GET /marketplace - Successfully retrieved {} items", dtos.size());
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("GET /marketplace - Bad request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /marketplace - Error listing items", e);
            return HttpResponse.badRequest(error("Failed to list marketplace items"));
        }
    }

    /**
     * GET /marketplace/{itemId}
     * Get a specific marketplace item by ID
     */
    @Get("/{itemId}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getItem(
            @PathVariable String itemId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            UUID itemUUID = UUID.fromString(itemId);
            log.info("GET /marketplace/{} - Getting item, user={}", itemId, userId);

            MarketplaceItem item = marketplaceService.getItem(itemUUID);
            ItemDTO dto = mapItemToDTO(item);

            log.info("GET /marketplace/{} - Item retrieved successfully", itemId);
            return HttpResponse.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("GET /marketplace/{} - Bad request: {}", itemId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /marketplace/{} - Error getting item", itemId, e);
            return HttpResponse.badRequest(error("Failed to get marketplace item"));
        }
    }

    /**
     * PUT /marketplace/{itemId}
     * Update a marketplace item
     */
    @Put("/{itemId}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> updateItem(
            @PathVariable String itemId,
            @Body UpdateItemRequest request,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            UUID itemUUID = UUID.fromString(itemId);
            log.info("PUT /marketplace/{} - Updating item, user={}", itemId, userId);

            MarketplaceItem item = marketplaceService.updateItem(itemUUID, request, userId);
            ItemDTO dto = mapItemToDTO(item);

            log.info("PUT /marketplace/{} - Item updated successfully", itemId);
            return HttpResponse.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("PUT /marketplace/{} - Bad request: {}", itemId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only update your own items")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PUT /marketplace/{} - Error updating item", itemId, e);
            return HttpResponse.badRequest(error("Failed to update marketplace item"));
        }
    }

    // ================= DTO Mapping Methods =================

    private ItemDTO mapItemToDTO(MarketplaceItem item) {
        ItemDTO dto = new ItemDTO();
        dto.setId(item.getId().toString());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setPrice(item.getPrice() != null ? item.getPrice().floatValue() : null);
        dto.setCategory(item.getCategory());
        
        // Map condition enum
        if (item.getCondition() != null) {
            try {
                dto.setCondition(com.anonymous.wall.model.ItemDTOCondition.fromValue(item.getCondition()));
            } catch (IllegalArgumentException e) {
                // If condition is not a valid enum value, leave it null
                log.warn("Invalid condition value: {}", item.getCondition());
            }
        }
        
        dto.setSold(item.isSold());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());

        // Set author info (following PostDTO pattern)
        com.anonymous.wall.model.ItemDTOAuthor author = new com.anonymous.wall.model.ItemDTOAuthor();
        author.setId(item.getUserId().toString());
        
        // Get user for author details
        Optional<UserEntity> userOpt = userRepository.findById(item.getUserId());
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            author.setProfileName(user.getProfileName());
        } else {
            author.setProfileName("Anonymous");
        }
        
        author.setIsAnonymous(false); // Marketplace items are not anonymous
        dto.setAuthor(author);

        return dto;
    }

    private Map<String, Object> createPaginationInfo(Page<?> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page.getPageNumber() + 1); // Convert from 0-based to 1-based
        pagination.put("limit", page.getSize());
        pagination.put("total", page.getTotalSize());
        pagination.put("totalPages", page.getTotalPages());
        return pagination;
    }

    private Map<String, String> error(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}
