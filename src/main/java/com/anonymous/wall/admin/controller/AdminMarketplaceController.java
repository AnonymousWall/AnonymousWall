package com.anonymous.wall.admin.controller;

import com.anonymous.wall.admin.service.AdminMarketplaceService;
import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.model.AdminMarketplaceDTO;
import com.anonymous.wall.model.AdminPostDTOWall;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller("/api/v1/admin/marketplaces")
public class AdminMarketplaceController {

    private static final Logger log = LoggerFactory.getLogger(AdminMarketplaceController.class);

    @Inject
    private AdminMarketplaceService adminMarketplaceService;

    private AdminMarketplaceDTO mapToDTO(MarketplaceItem item) {
        AdminMarketplaceDTO dto = new AdminMarketplaceDTO();
        dto.setId(item.getId());
        dto.setUserId(item.getUserId());
        dto.setProfileName(item.getProfileName());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setPrice(item.getPrice());
        dto.setCategory(item.getCategory());
        dto.setCondition(item.getCondition());
        dto.setSold(item.isSold());
        dto.setWall(AdminPostDTOWall.fromValue(item.getWall()));
        dto.setSchoolDomain(item.getSchoolDomain());
        dto.setCommentCount(item.getCommentCount());
        dto.setHidden(item.isHidden());
        dto.setImageUrls(item.getImageUrls());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }

    @Get
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getAllMarketplaces(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String userId,
            @Nullable @QueryValue Boolean hidden,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder) {

        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;

        Pageable pageable = Pageable.from(page - 1, limit);
        UUID userIdUuid = userId != null ? UUID.fromString(userId) : null;
        Page<MarketplaceItem> itemsPage = adminMarketplaceService.getAllMarketplaces(pageable, userIdUuid, hidden, sortBy, sortOrder);

        List<AdminMarketplaceDTO> dtos = itemsPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", dtos);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", itemsPage.getTotalSize());
        pagination.put("totalPages", itemsPage.getTotalPages());
        response.put("pagination", pagination);

        return HttpResponse.ok(response);
    }

    @Get("/{id}")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<AdminMarketplaceDTO> getMarketplaceById(@PathVariable String id) {
        UUID itemId = UUID.fromString(id);
        return HttpResponse.ok(mapToDTO(adminMarketplaceService.getMarketplaceById(itemId)));
    }

    @Put("/{id}/hide")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> hideMarketplace(@PathVariable String id) {
        log.info("Admin hiding marketplace item: {}", id);
        adminMarketplaceService.hideMarketplace(UUID.fromString(id));
        Map<String, String> response = new HashMap<>();
        response.put("message", "Marketplace item hidden successfully");
        return HttpResponse.ok(response);
    }

    @Put("/{id}/unhide")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> unhideMarketplace(@PathVariable String id) {
        log.info("Admin unhiding marketplace item: {}", id);
        adminMarketplaceService.unhideMarketplace(UUID.fromString(id));
        Map<String, String> response = new HashMap<>();
        response.put("message", "Marketplace item unhidden successfully");
        return HttpResponse.ok(response);
    }

    @Get("/{id}/images")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getMarketplaceImages(@PathVariable String id) {
        UUID itemId = UUID.fromString(id);
        List<String> imageUrls = adminMarketplaceService.getMarketplaceImages(itemId);
        Map<String, Object> response = new HashMap<>();
        response.put("marketplaceItemId", itemId);
        response.put("imageUrls", imageUrls);
        return HttpResponse.ok(response);
    }
}
