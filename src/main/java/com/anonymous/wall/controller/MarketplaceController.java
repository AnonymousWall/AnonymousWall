package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.*;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.CommentsService;
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
    private CommentsService commentsService;

    @Inject
    private UserRepository userRepository;

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

    private String getSchoolDomainFromRequest(HttpRequest<?> request) {
        Optional<Principal> principalOpt = request.getUserPrincipal();
        if (principalOpt.isEmpty()) {
            return null;
        }
        Principal principal = principalOpt.get();
        if (principal instanceof io.micronaut.security.authentication.Authentication) {
            io.micronaut.security.authentication.Authentication auth = (io.micronaut.security.authentication.Authentication) principal;
            Object schoolDomainObj = auth.getAttributes().get("schoolDomain");
            return schoolDomainObj != null ? schoolDomainObj.toString() : null;
        }
        return null;
    }

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

    @Get
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> listItems(
            @QueryValue(defaultValue = "campus") String wall,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @QueryValue(defaultValue = "newest") String sortBy,
            @Nullable @QueryValue Boolean sold,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            String schoolDomain = getSchoolDomainFromRequest(httpRequest);
            log.info("GET /marketplace - Listing items, user={}, wall={}, page={}, limit={}, sortBy={}, sold={}", 
                    userId, wall, page, limit, sortBy, sold);

            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            Pageable pageable = Pageable.from(page - 1, limit);
            Page<MarketplaceItem> items = marketplaceService.getItemsByWall(wall, pageable, userId, schoolDomain, sortBy, sold);

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

    @Get("/{itemId}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getItem(
            @PathVariable String itemId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            UUID itemUUID = UUID.fromString(itemId);
            log.info("GET /marketplace/{} - Getting item, user={}", itemId, userId);
            MarketplaceItem item = marketplaceService.getItem(itemUUID, userId);
            ItemDTO dto = mapItemToDTO(item);
            log.info("GET /marketplace/{} - Item retrieved successfully", itemId);
            return HttpResponse.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("GET /marketplace/{} - Bad request: {}", itemId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /marketplace/{} - Error getting item", itemId, e);
            return HttpResponse.badRequest(error("Failed to get marketplace item"));
        }
    }

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

    @Patch("/{itemId}/hide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> hideItem(
            @PathVariable UUID itemId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /marketplace/{}/hide - Hiding item, user={}", itemId, userId);
            marketplaceService.hideItem(itemId, userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Item hidden successfully");
            log.info("PATCH /marketplace/{}/hide - Item hidden successfully", itemId);
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /marketplace/{}/hide - Bad request: {}", itemId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only hide your own items")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /marketplace/{}/hide - Error hiding item", itemId, e);
            return HttpResponse.badRequest(error("Failed to hide marketplace item"));
        }
    }

    @Patch("/{itemId}/unhide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> unhideItem(
            @PathVariable UUID itemId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /marketplace/{}/unhide - Unhiding item, user={}", itemId, userId);
            marketplaceService.unhideItem(itemId, userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Item unhidden successfully");
            log.info("PATCH /marketplace/{}/unhide - Item unhidden successfully", itemId);
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /marketplace/{}/unhide - Bad request: {}", itemId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only unhide your own items")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /marketplace/{}/unhide - Error unhiding item", itemId, e);
            return HttpResponse.badRequest(error("Failed to unhide marketplace item"));
        }
    }

    // ================= Comment Endpoints =================

    @io.micronaut.http.annotation.Post("/{itemId}/comments")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> addComment(
            @PathVariable UUID itemId,
            @Body CreateCommentRequest request,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("POST /marketplace/{}/comments - Adding comment, user={}", itemId, userId);
            Comment comment = commentsService.addComment(CommentParentType.MARKETPLACE, itemId, request, userId);
            CommentDTO dto = mapCommentToDTO(comment);
            log.info("POST /marketplace/{}/comments - Comment added successfully, commentId={}", itemId, dto.getId());
            return HttpResponse.created(dto);
        } catch (IllegalArgumentException e) {
            log.warn("POST /marketplace/{}/comments - Bad request: {}", itemId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /marketplace/{}/comments - Error adding comment", itemId, e);
            return HttpResponse.badRequest(error("Failed to add comment"));
        }
    }

    @Get("/{itemId}/comments")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getComments(
            @PathVariable UUID itemId,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @QueryValue(defaultValue = "NEWEST") String sort,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("GET /marketplace/{}/comments - Getting comments, user={}", itemId, userId);

            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            marketplaceService.getItem(itemId, userId);

            Pageable pageable = Pageable.from(page - 1, limit);
            SortBy sortBy = SortBy.parse(sort);
            Page<Comment> commentPage = commentsService.getCommentsWithPagination(CommentParentType.MARKETPLACE, itemId, pageable, sortBy);

            List<CommentDTO> dtos = commentPage.getContent().stream()
                    .map(this::mapCommentToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("limit", limit);
            pagination.put("total", commentPage.getTotalSize());
            pagination.put("totalPages", commentPage.getTotalPages());

            Map<String, Object> response = new HashMap<>();
            response.put("data", dtos);
            response.put("pagination", pagination);

            log.info("GET /marketplace/{}/comments - Successfully retrieved {} comments", itemId, dtos.size());
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("GET /marketplace/{}/comments - Bad request: {}", itemId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /marketplace/{}/comments - Error getting comments", itemId, e);
            return HttpResponse.badRequest(error("Failed to get comments"));
        }
    }

    @Patch("/{itemId}/comments/{commentId}/hide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> hideComment(
            @PathVariable UUID itemId,
            @PathVariable UUID commentId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /marketplace/{}/comments/{}/hide - Hiding comment, user={}", itemId, commentId, userId);
            commentsService.hideComment(CommentParentType.MARKETPLACE, itemId, commentId, userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Comment hidden successfully");
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /marketplace/{}/comments/{}/hide - Bad request: {}", itemId, commentId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only hide your own comments") || e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /marketplace/{}/comments/{}/hide - Error hiding comment", itemId, commentId, e);
            return HttpResponse.badRequest(error("Failed to hide comment"));
        }
    }

    @Patch("/{itemId}/comments/{commentId}/unhide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> unhideComment(
            @PathVariable UUID itemId,
            @PathVariable UUID commentId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /marketplace/{}/comments/{}/unhide - Unhiding comment, user={}", itemId, commentId, userId);
            commentsService.unhideComment(CommentParentType.MARKETPLACE, itemId, commentId, userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Comment unhidden successfully");
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /marketplace/{}/comments/{}/unhide - Bad request: {}", itemId, commentId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only unhide your own comments") || e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /marketplace/{}/comments/{}/unhide - Error unhiding comment", itemId, commentId, e);
            return HttpResponse.badRequest(error("Failed to unhide comment"));
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

        if (item.getCondition() != null) {
            try {
                dto.setCondition(ItemDTOCondition.fromValue(item.getCondition()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid condition value: {}", item.getCondition());
            }
        }

        dto.setSold(item.isSold());

        if (item.getWall() != null) {
            dto.setWall(ItemDTOWall.valueOf(item.getWall().toUpperCase()));
        }
        dto.setComments(item.getCommentCount());

        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());

        ItemDTOAuthor author = new ItemDTOAuthor();
        author.setId(item.getUserId().toString());

        Optional<UserEntity> userOpt = userRepository.findById(item.getUserId());
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            author.setProfileName(user.getProfileName());
        } else {
            author.setProfileName("Anonymous");
        }

        author.setIsAnonymous(false);
        dto.setAuthor(author);

        return dto;
    }

    private CommentDTO mapCommentToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getParentId());
        if (comment.getParentType() != null) {
            dto.setParentType(CommentDTOParentType.valueOf(comment.getParentType()));
        }
        dto.setText(comment.getText());
        dto.setCreatedAt(comment.getCreatedAt());

        CommentDTOAuthor author = new CommentDTOAuthor();
        author.setId(comment.getUserId().toString());
        author.setProfileName(comment.getProfileName());
        author.setIsAnonymous(true);
        dto.setAuthor(author);

        return dto;
    }

    private Map<String, Object> createPaginationInfo(Page<?> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page.getPageNumber() + 1);
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
