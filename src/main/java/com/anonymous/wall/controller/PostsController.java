package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.model.*;
import com.anonymous.wall.service.retry.PollRetryService;
import com.anonymous.wall.service.retry.PostsRetryService;
import com.anonymous.wall.service.retry.CommentsRetryService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.multipart.CompletedPart;
import io.micronaut.http.server.multipart.MultipartBody;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@Controller("/api/v1/posts")
public class PostsController {

    private static final Logger log = LoggerFactory.getLogger(PostsController.class);

    @Inject
    private PostsRetryService postsRetryService;

    @Inject
    private PollRetryService pollRetryService;

    @Inject
    private CommentsRetryService commentsRetryService;

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

    // Helper to extract schoolDomain from JWT attributes
    private String getSchoolDomainFromRequest(HttpRequest<?> request) {
        Optional<Principal> principalOpt = request.getUserPrincipal();

        if (principalOpt.isEmpty()) {
            return null;
        }

        Principal principal = principalOpt.get();
        // In Micronaut Security with JWT, the principal has attributes from claims
        if (principal instanceof io.micronaut.security.authentication.Authentication) {
            io.micronaut.security.authentication.Authentication auth = (io.micronaut.security.authentication.Authentication) principal;
            Object schoolDomainObj = auth.getAttributes().get("schoolDomain");
            return schoolDomainObj != null ? schoolDomainObj.toString() : null;
        }
        
        return null;
    }

    /**
     * POST /posts
     * Create a new post (multipart/form-data with optional images)
     */
    @io.micronaut.http.annotation.Post(consumes = MediaType.MULTIPART_FORM_DATA)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<Object> createPost(
            @Body MultipartBody body,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("POST /posts - Creating new post, user={}", userId);
            // Safe to block here because @ExecuteOn(BLOCKING) moves us off the event loop
            List<CompletedPart> parts = Flux.from(body).collectList().block();

            String title = null;
            String content = null;
            String wall = null;
            String postType = null;
            List<String> pollOptions = new ArrayList<>();
            List<CompletedFileUpload> images = new ArrayList<>();

            for (CompletedPart part : parts) {
                if (part instanceof CompletedFileUpload file) {
                    if ("images".equals(file.getName()) && file.getSize() > 0) {
                        images.add(file);
                    }
                } else {
                    String value = new String(part.getBytes(), StandardCharsets.UTF_8);
                    switch (part.getName()) {
                        case "title"        -> title = value;
                        case "content"      -> content = value;
                        case "wall"         -> wall = value;
                        case "postType"     -> postType = value;
                        case "pollOptions"  -> pollOptions.add(value);
                    }
                }
            }

            log.info("POST /posts - user={}, content_length={}, imageCount={}, postType={}",
                    userId, content != null ? content.length() : 0, images.size(), postType);

            if (title == null || title.isBlank()) {
                return HttpResponse.badRequest(error("Title is required"));
            }

            boolean isPoll = "poll".equalsIgnoreCase(postType);

            // For standard posts, content is required
            if (!isPoll && (content == null || content.isBlank())) {
                return HttpResponse.badRequest(error("Content is required"));
            }

            // Validate poll options early
            if (isPoll) {
                if (pollOptions.size() < 2) {
                    return HttpResponse.badRequest(error("Poll must have at least 2 options"));
                }
                if (pollOptions.size() > 4) {
                    return HttpResponse.badRequest(error("Poll cannot have more than 4 options"));
                }
                for (String opt : pollOptions) {
                    if (opt == null || opt.trim().isEmpty()) {
                        return HttpResponse.badRequest(error("Poll option text cannot be empty"));
                    }
                    if (opt.trim().length() > 100) {
                        return HttpResponse.badRequest(error("Poll option text exceeds maximum length of 100 characters"));
                    }
                }
            }

            CreatePostRequest request = new CreatePostRequest(title, content != null ? content : "");
            if (wall != null && !wall.isEmpty()) {
                try {
                    request.setWall(CreatePostRequestWall.fromValue(wall.toLowerCase()));
                } catch (IllegalArgumentException e) {
                    return HttpResponse.badRequest(error("Wall must be 'campus' or 'national'"));
                }
            }
            if (isPoll) {
                request.setPostType(CreatePostRequestPostType.POLL);
                request.setPollOptions(pollOptions);
            }

            Post post = postsRetryService.createPost(request, images, userId);

            PostDTO dto = mapPostToDTO(post, userId);

            log.info("POST /posts - Post created successfully, postId={}", dto.getId());
            return HttpResponse.created(dto);

        } catch (IllegalArgumentException e) {
            log.warn("POST /posts - Bad request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /posts - Error creating post", e);
            return HttpResponse.badRequest(error("Failed to create post"));
        }
    }


    /**
     * GET /posts/{postId}
     * Get a single post by ID
     */
    @Get("/{postId}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getPostById(@PathVariable UUID postId, HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("GET /posts/{} - Getting post, user={}", postId, userId);

            Post post = postsRetryService.getPost(postId, userId);
            PostDTO dto = mapPostToDTO(post, userId);

            log.info("GET /posts/{} - Post retrieved successfully", postId);
            return HttpResponse.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("GET /posts/{} - Bad request: {}", postId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /posts/{} - Error getting post", postId, e);
            return HttpResponse.badRequest(error("Failed to get post"));
        }
    }

    /**
     * GET /posts
     * List posts with optional wall filter, pagination, and sorting
     * Query parameters: wall (default campus), page (default 1), limit (default 20), sort (default NEWEST)
     * Sort options: NEWEST, OLDEST, MOST_LIKED, LEAST_LIKED
     */
    @Get
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> listPosts(
            @QueryValue(defaultValue = "campus") String wall,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @QueryValue(defaultValue = "NEWEST") String sort,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            String schoolDomain = getSchoolDomainFromRequest(httpRequest);
            log.info("GET /posts - Listing posts, user={}, wall={}, page={}, limit={}, sort={}, schoolDomain={}", userId, wall, page, limit, sort, schoolDomain);

            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            Pageable pageable = Pageable.from(page - 1, limit);

            com.anonymous.wall.model.SortBy sortBy = com.anonymous.wall.model.SortBy.parse(sort);
            
            // Use optimized method that doesn't require user lookup
            Page<Post> posts = postsRetryService.getPostsByWall(wall, pageable, userId, schoolDomain, sortBy);

            List<PostDTO> dtos = posts.getContent().stream()
                    .map(p -> mapPostToDTO(p, userId))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", dtos);
            response.put("pagination", createPaginationInfo(posts));

            log.info("GET /posts - Successfully retrieved {} posts", dtos.size());
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("GET /posts - Bad request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /posts - Error listing posts", e);
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
            @PathVariable UUID postId,
            @Body CreateCommentRequest request,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("POST /posts/{}/comments - Adding comment, user={}, text_length={}", postId, userId, request.getText().length());

            Comment comment = commentsRetryService.addComment(CommentParentType.POST, postId, request, userId);
            CommentDTO dto = mapCommentToDTO(comment);

            log.info("POST /posts/{}/comments - Comment added successfully, commentId={}", postId, dto.getId());
            return HttpResponse.created(dto);
        } catch (IllegalArgumentException e) {
            log.warn("POST /posts/{}/comments - Bad request: {}", postId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /posts/{}/comments - Error adding comment", postId, e);
            return HttpResponse.badRequest(error("Failed to add comment"));
        }
    }

    /**
     * GET /posts/{postId}/comments
     * Get comments for a post with optional pagination and sorting
     * Query parameters: page (default 1), limit (default 20), sort (default NEWEST)
     * Sort options: NEWEST, OLDEST (comments only sort by creation time)
     */
    @Get("/{postId}/comments")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getComments(
            @PathVariable UUID postId,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @QueryValue(defaultValue = "NEWEST") String sort,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("GET /posts/{}/comments - Getting comments, user={}, page={}, limit={}, sort={}", postId, userId, page, limit, sort);

            // Validate pagination parameters
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            // This will validate visibility and throw if user doesn't have access
            postsRetryService.getPost(postId, userId);

            Pageable pageable = Pageable.from(page - 1, limit);
            com.anonymous.wall.model.SortBy sortBy = com.anonymous.wall.model.SortBy.parse(sort);
            Page<Comment> commentPage = commentsRetryService.getCommentsWithPagination(CommentParentType.POST, postId, pageable, sortBy, userId);

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

            log.info("GET /posts/{}/comments - Successfully retrieved {} comments", postId, dtos.size());
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("GET /posts/{}/comments - Bad request: {}", postId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /posts/{}/comments - Error getting comments", postId, e);
            return HttpResponse.badRequest(error("Failed to get comments"));
        }
    }

    /**
     * POST /posts/{postId}/likes
     * Toggle like on a post
     */
    @io.micronaut.http.annotation.Post("/{postId}/likes")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> likePost(@PathVariable UUID postId, HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("POST /posts/{}/likes - Toggling like, user={}", postId, userId);

            Map<String, Object> result = postsRetryService.toggleLikeWithDetails(postId, userId);

            log.info("POST /posts/{}/likes - Like toggled successfully, liked={}, likeCount={}", postId, result.get("liked"), result.get("likeCount"));
            return HttpResponse.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("POST /posts/{}/likes - Bad request: {}", postId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /posts/{}/likes - Error liking post", postId, e);
            return HttpResponse.badRequest(error("Failed to like post"));
        }
    }

    /**
     * PATCH /posts/{postId}/comments/{commentId}/hide
     * Hide a comment
     */
    @Patch("/{postId}/comments/{commentId}/hide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> hideComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /posts/{}/comments/{}/hide - Hiding comment, user={}", postId, commentId, userId);

            commentsRetryService.hideComment(CommentParentType.POST, postId, commentId, userId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Comment hidden successfully");

            log.info("PATCH /posts/{}/comments/{}/hide - Comment hidden successfully", postId, commentId);
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /posts/{}/comments/{}/hide - Bad request: {}", postId, commentId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only hide your own comments") ||
                e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /posts/{}/comments/{}/hide - Error hiding comment", postId, commentId, e);
            return HttpResponse.badRequest(error("Failed to hide comment"));
        }
    }

    /**
     * PATCH /posts/{postId}/comments/{commentId}/unhide
     * Unhide a comment
     */
    @Patch("/{postId}/comments/{commentId}/unhide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> unhideComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /posts/{}/comments/{}/unhide - Unhiding comment, user={}", postId, commentId, userId);

            commentsRetryService.unhideComment(CommentParentType.POST, postId, commentId, userId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Comment unhidden successfully");

            log.info("PATCH /posts/{}/comments/{}/unhide - Comment unhidden successfully", postId, commentId);
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /posts/{}/comments/{}/unhide - Bad request: {}", postId, commentId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only unhide your own comments") ||
                e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /posts/{}/comments/{}/unhide - Error unhiding comment", postId, commentId, e);
            return HttpResponse.badRequest(error("Failed to unhide comment"));
        }
    }

    /**
     * PATCH /posts/{postId}/hide
     * Hide a post
     */
    @Patch("/{postId}/hide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> hidePost(
            @PathVariable UUID postId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /posts/{}/hide - Hiding post, user={}", postId, userId);

            postsRetryService.hidePost(postId, userId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Post hidden successfully");

            log.info("PATCH /posts/{}/hide - Post hidden successfully", postId);
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /posts/{}/hide - Bad request: {}", postId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only hide your own posts")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /posts/{}/hide - Error hiding post", postId, e);
            return HttpResponse.badRequest(error("Failed to hide post"));
        }
    }

    /**
     * PATCH /posts/{postId}/unhide
     * Unhide a post
     */
    @Patch("/{postId}/unhide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> unhidePost(
            @PathVariable UUID postId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /posts/{}/unhide - Unhiding post, user={}", postId, userId);

            postsRetryService.unhidePost(postId, userId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Post unhidden successfully");

            log.info("PATCH /posts/{}/unhide - Post unhidden successfully", postId);
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /posts/{}/unhide - Bad request: {}", postId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only unhide your own posts")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /posts/{}/unhide - Error unhiding post", postId, e);
            return HttpResponse.badRequest(error("Failed to unhide post"));
        }
    }

    /**
     * POST /posts/{postId}/reports
     * Report a post
     */
    @io.micronaut.http.annotation.Post("/{postId}/reports")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> reportPost(
            @PathVariable UUID postId,
            @Body com.anonymous.wall.model.ReportRequest request,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("POST /posts/{}/reports - Reporting post, user={}", postId, userId);

            String reason = request != null && request.getReason() != null ? request.getReason() : null;
            postsRetryService.reportPost(postId, userId, reason);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Post reported successfully");

            log.info("POST /posts/{}/reports - Post reported successfully", postId);
            return HttpResponse.created(response);
        } catch (IllegalArgumentException e) {
            log.warn("POST /posts/{}/reports - Bad request: {}", postId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /posts/{}/reports - Error reporting post", postId, e);
            return HttpResponse.badRequest(error("Failed to report post"));
        }
    }

    /**
     * POST /posts/{postId}/comments/{commentId}/reports
     * Report a comment
     */
    @io.micronaut.http.annotation.Post("/{postId}/comments/{commentId}/reports")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> reportComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Body com.anonymous.wall.model.ReportRequest request,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("POST /posts/{}/comments/{}/reports - Reporting comment, user={}", postId, commentId, userId);

            String reason = request != null && request.getReason() != null ? request.getReason() : null;
            commentsRetryService.reportComment(commentId, userId, reason);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Comment reported successfully");

            log.info("POST /posts/{}/comments/{}/reports - Comment reported successfully", postId, commentId);
            return HttpResponse.created(response);
        } catch (IllegalArgumentException e) {
            log.warn("POST /posts/{}/comments/{}/reports - Bad request: {}", postId, commentId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /posts/{}/comments/{}/reports - Error reporting comment", postId, commentId, e);
            return HttpResponse.badRequest(error("Failed to report comment"));
        }
    }

    // ================= DTO Mapping Methods =================

    private PostDTO mapPostToDTO(Post post, UUID currentUserId) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setWall(PostDTOWall.valueOf(post.getWall().toUpperCase()));
        dto.setLikes(post.getLikeCount());
        dto.setComments(post.getCommentCount());
        dto.setLiked(post.isLiked());
        dto.setImageUrls(post.getImageUrls());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());

        // Set post type
        String postTypeStr = post.getPostType() != null ? post.getPostType() : "standard";
        dto.setPostType(PostDTOPostType.fromValue(postTypeStr));

        // Set total votes for poll posts
        if ("poll".equals(postTypeStr)) {
            dto.setTotalVotes(post.getTotalVotes());
            // Include poll data if userId is available
            if (currentUserId != null) {
                try {
                    Map<String, Object> pollData = pollRetryService.getPollData(post.getId(), currentUserId, false);
                    PollDTO pollDTO = buildPollDTO(pollData);
                    dto.setPoll(pollDTO);
                } catch (Exception e) {
                    log.warn("Failed to load poll data for post={}: {}", post.getId(), e.getMessage());
                }
            }
        } else {
            dto.setTotalVotes(null);
            dto.setPoll(null);
        }

        // Set author info (anonymous)
        PostDTOAuthor author = new PostDTOAuthor();
        author.setId(post.getUserId().toString());
        author.setProfileName(post.getProfileName());
        author.setIsAnonymous(true); // All posts are anonymous
        dto.setAuthor(author);

        return dto;
    }

    @SuppressWarnings("unchecked")
    private PollDTO buildPollDTO(Map<String, Object> pollData) {
        PollDTO pollDTO = new PollDTO();
        pollDTO.setTotalVotes((Integer) pollData.get("totalVotes"));
        Object uvoi = pollData.get("userVotedOptionId");
        pollDTO.setUserVotedOptionId(uvoi instanceof UUID ? (UUID) uvoi : null);
        pollDTO.setResultsVisible((Boolean) pollData.get("resultsVisible"));

        List<Map<String, Object>> optionMaps = (List<Map<String, Object>>) pollData.get("options");
        List<PollOptionDTO> optionDTOs = new ArrayList<>();
        if (optionMaps != null) {
            for (Map<String, Object> optMap : optionMaps) {
                PollOptionDTO optDTO = new PollOptionDTO();
                Object idObj = optMap.get("id");
                optDTO.setId(idObj instanceof UUID ? (UUID) idObj : UUID.fromString(idObj.toString()));
                optDTO.setOptionText((String) optMap.get("optionText"));
                optDTO.setDisplayOrder((Integer) optMap.get("displayOrder"));
                Object vc = optMap.get("voteCount");
                optDTO.setVoteCount(vc instanceof Integer ? (Integer) vc : null);
                Object pct = optMap.get("percentage");
                optDTO.setPercentage(pct instanceof Number ? ((Number) pct).doubleValue() : null);
                optionDTOs.add(optDTO);
            }
        }
        pollDTO.setOptions(optionDTOs);
        return pollDTO;
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

        // Set author info (anonymous)
        CommentDTOAuthor author = new CommentDTOAuthor();
        author.setId(comment.getUserId().toString());
        author.setProfileName(comment.getProfileName());
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
