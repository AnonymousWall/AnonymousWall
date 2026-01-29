package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.model.*;
import com.anonymous.wall.service.PostsService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller("/api/v1/posts")
public class PostsController {

    private static final Logger log = LoggerFactory.getLogger(PostsController.class);

    @Inject
    private PostsService postsService;

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
     * POST /posts
     * Create a new post
     */
    @io.micronaut.http.annotation.Post
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> createPost(@Body CreatePostRequest request, HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            Post post = postsService.createPost(request, userId);
            PostDTO dto = mapPostToDTO(post);
            return HttpResponse.created(dto);
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating post", e);
            return HttpResponse.badRequest(error("Failed to create post"));
        }
    }

    /**
     * GET /posts
     * List posts with optional wall filter and pagination
     */
    @Get
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> listPosts(
            @QueryValue(defaultValue = "campus") String wall,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            HttpRequest<?> httpRequest) {
        try {
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            UUID userId = getUserIdFromRequest(httpRequest);
            Pageable pageable = Pageable.from(page - 1, limit);
            Page<Post> posts = postsService.getPostsByWall(wall, pageable, userId);

            List<PostDTO> dtos = posts.getContent().stream()
                    .map(this::mapPostToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", dtos);
            response.put("pagination", createPaginationInfo(posts));

            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error listing posts", e);
            return HttpResponse.badRequest(error("Failed to list posts"));
        }
    }

    /**
     * POST /posts/{postId}/comments
     * Add a comment to a post
     */
    @io.micronaut.http.annotation.Post("/{postId}/comments")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> addComment(
            @PathVariable Long postId,
            @Body CreateCommentRequest request,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            Comment comment = postsService.addComment(postId, request, userId);
            CommentDTO dto = mapCommentToDTO(comment);
            return HttpResponse.created(dto);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.<Object>status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding comment", e);
            return HttpResponse.badRequest(error("Failed to add comment"));
        }
    }

    /**
     * GET /posts/{postId}/comments
     * Get all comments for a post
     */
    @Get("/{postId}/comments")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getComments(@PathVariable Long postId, HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            // This will validate visibility and throw if user doesn't have access
            postsService.getPost(postId, userId);
            List<Comment> comments = postsService.getComments(postId);
            List<CommentDTO> dtos = comments.stream()
                    .map(this::mapCommentToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", dtos);
            response.put("total", dtos.size());

            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.<Object>status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting comments", e);
            return HttpResponse.badRequest(error("Failed to get comments"));
        }
    }

    /**
     * POST /posts/{postId}/likes
     * Toggle like on a post
     */
    @io.micronaut.http.annotation.Post("/{postId}/likes")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> likePost(@PathVariable Long postId, HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            boolean isNowLiked = postsService.toggleLike(postId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("liked", isNowLiked);

            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.<Object>status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error liking post", e);
            return HttpResponse.badRequest(error("Failed to like post"));
        }
    }

    // ================= DTO Mapping Methods =================

    private PostDTO mapPostToDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId().toString());
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
        author.setIsAnonymous(true); // All posts are anonymous
        dto.setAuthor(author);

        return dto;
    }

    private CommentDTO mapCommentToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId().toString());
        dto.setPostId(comment.getPostId().toString());
        dto.setText(comment.getText());
        dto.setCreatedAt(comment.getCreatedAt());

        // Set author info (anonymous)
        CommentDTOAuthor author = new CommentDTOAuthor();
        author.setId(comment.getUserId().toString());
        author.setIsAnonymous(true); // All comments are anonymous
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


