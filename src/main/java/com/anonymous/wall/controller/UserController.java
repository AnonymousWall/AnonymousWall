package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.mapper.UserMapper;
import com.anonymous.wall.model.*;
import com.anonymous.wall.service.CommentsService;
import com.anonymous.wall.service.UserService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;
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

    @Inject
    private com.anonymous.wall.service.PostsService postsService;

    @Inject
    private UserService userService;

    @Inject
    private UserMapper userMapper;

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
     * Query parameters: page (default 1), limit (default 20), sort (default NEWEST)
     * Sort options: NEWEST, OLDEST (comments only sort by creation time)
     * Hidden comments are excluded (soft-deleted comments are not shown)
     */
    @Get("/me/comments")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getUserComments(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @QueryValue(defaultValue = "NEWEST") String sort,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("GET /users/me/comments - Getting user's own comments, user={}, page={}, limit={}, sort={}", 
                userId, page, limit, sort);

            // Validate pagination parameters
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            Pageable pageable = Pageable.from(page - 1, limit);
            SortBy sortBy = SortBy.parse(sort);
            Page<Comment> commentPage = commentsService.getUserOwnComments(userId, pageable, sortBy);

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

    /**
     * GET /users/me/posts
     * Get current user's own posts with pagination and sorting
     * Query parameters: page (default 1), limit (default 20), sort (default NEWEST)
     * Sort options: NEWEST, OLDEST, MOST_LIKED, LEAST_LIKED
     * Hidden posts are excluded (soft-deleted posts are not shown)
     */
    @Get("/me/posts")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getUserPosts(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @QueryValue(defaultValue = "NEWEST") String sort,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("GET /users/me/posts - Getting user's own posts, user={}, page={}, limit={}, sort={}", 
                userId, page, limit, sort);

            // Validate pagination parameters
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            Pageable pageable = Pageable.from(page - 1, limit);
            SortBy sortBy = SortBy.parse(sort);
            Page<Post> postPage = postsService.getUserOwnPosts(userId, pageable, sortBy);

            List<PostDTO> dtos = postPage.getContent().stream()
                    .map(this::mapPostToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("limit", limit);
            pagination.put("total", postPage.getTotalSize());
            pagination.put("totalPages", postPage.getTotalPages());

            Map<String, Object> response = new HashMap<>();
            response.put("data", dtos);
            response.put("pagination", pagination);

            log.info("GET /users/me/posts - Successfully retrieved {} posts", dtos.size());
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("GET /users/me/posts - Bad request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /users/me/posts - Error getting user posts", e);
            return HttpResponse.badRequest(error("Failed to get user posts"));
        }
    }

    /**
     * PATCH /users/me/profile/name
     * Update user profile name with async propagation to posts and comments
     */
    @Patch("/me/profile/name")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> updateProfileName(@Body UpdateProfileNameRequest updateProfileNameRequest,
                                                  HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /users/me/profile/name - Updating profile name for user: {}", userId);

            String newProfileName = updateProfileNameRequest.getProfileName();

            // Use UserService which handles async propagation via events
            UserEntity updatedUser = userService.updateProfileName(userId, newProfileName);

            log.info("PATCH /users/me/profile/name - Profile name updated successfully for user: {}, newName={}", 
                    userId, updatedUser.getProfileName());
            return HttpResponse.ok(userMapper.toDTO(updatedUser));
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /users/me/profile/name - Invalid request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /users/me/profile/name - Error updating profile name", e);
            return HttpResponse.badRequest(error("Failed to update profile name"));
        }
    }

    // ================= DTO Mapping Methods =================

    private CommentDTO mapCommentToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getParentId());
        if (comment.getParentType() != null) {
            dto.setParentType(CommentDTOParentType.valueOf(comment.getParentType()));
        }
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

    private PostDTO mapPostToDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setWall(PostDTOWall.valueOf(post.getWall().toUpperCase()));
        dto.setLikes(post.getLikeCount());
        dto.setComments(post.getCommentCount());
        dto.setLiked(post.isLiked());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());

        // Set author info (anonymous)
        PostDTOAuthor author = new PostDTOAuthor();
        author.setId(post.getUserId().toString());
        author.setProfileName(post.getProfileName());
        author.setIsAnonymous(true); // All posts are anonymous
        dto.setAuthor(author);

        return dto;
    }

    private Map<String, String> error(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}
