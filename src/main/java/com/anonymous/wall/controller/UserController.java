package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.model.CommentDTO;
import com.anonymous.wall.model.CommentDTOAuthor;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.service.CommentsService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller("/api/v1/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Inject
    private CommentsService commentsService;

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
     * GET /users/me/comments
     * Get current user's own comments with pagination and sorting
     * Query parameters: page (default 1), limit (default 20), sort (default NEWEST), includeHidden (default false)
     * Sort options: NEWEST, OLDEST (comments only sort by creation time)
     */
    @Get("/me/comments")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getUserComments(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @QueryValue(defaultValue = "NEWEST") String sort,
            @QueryValue(defaultValue = "false") boolean includeHidden,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("GET /users/me/comments - Getting user's own comments, user={}, page={}, limit={}, sort={}, includeHidden={}", 
                userId, page, limit, sort, includeHidden);

            // Validate pagination parameters
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            Pageable pageable = Pageable.from(page - 1, limit);
            SortBy sortBy = SortBy.parse(sort);
            Page<Comment> commentPage = commentsService.getUserOwnComments(userId, pageable, sortBy, includeHidden);

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

            log.info("GET /users/me/comments - Successfully retrieved {} comments", dtos.size());
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("GET /users/me/comments - Bad request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /users/me/comments - Error getting user comments", e);
            return HttpResponse.badRequest(error("Failed to get user comments"));
        }
    }

    // ================= DTO Mapping Methods =================

    private CommentDTO mapCommentToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPostId());
        dto.setText(comment.getText());
        dto.setCreatedAt(comment.getCreatedAt());

        // Set author info (anonymous)
        CommentDTOAuthor author = new CommentDTOAuthor();
        author.setId(comment.getUserId().toString());
        author.setProfileName(comment.getProfileName());
        author.setIsAnonymous(true); // All comments are anonymous
        dto.setAuthor(author);

        return dto;
    }

    private Map<String, String> error(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}
