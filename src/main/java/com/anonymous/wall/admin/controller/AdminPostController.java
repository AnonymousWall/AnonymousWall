package com.anonymous.wall.admin.controller;

import com.anonymous.wall.admin.service.AdminPostService;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.model.AdminCommentDTO;
import com.anonymous.wall.model.AdminPostDTO;
import com.anonymous.wall.model.AdminPostDTOWall;
import com.anonymous.wall.model.PostDTOPostType;
import com.anonymous.wall.model.SortBy;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin controller for post moderation
 */
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("/api/v1/admin/posts")
public class AdminPostController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminPostController.class);
    
    @Inject
    private AdminPostService adminPostService;
    
    private AdminPostDTO mapPostToDTO(Post post) {
        AdminPostDTO dto = new AdminPostDTO();
        dto.setId(post.getId());
        dto.setUserId(post.getUserId());
        dto.setProfileName(post.getProfileName());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setWall(AdminPostDTOWall.fromValue(post.getWall()));
        dto.setSchoolDomain(post.getSchoolDomain());
        dto.setLikeCount(post.getLikeCount());
        dto.setCommentCount(post.getCommentCount());
        dto.setHidden(post.isHidden());
        dto.setImageUrls(post.getImageUrls());
        dto.setPostType(PostDTOPostType.fromValue(post.getPostType() != null ? post.getPostType() : "standard"));
        dto.setTotalVotes(post.getTotalVotes());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        return dto;
    }

    private AdminCommentDTO mapCommentToDTO(Comment comment) {
        AdminCommentDTO dto = new AdminCommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getParentId());
        dto.setUserId(comment.getUserId());
        dto.setProfileName(comment.getProfileName());
        dto.setText(comment.getText());
        dto.setHidden(comment.isHidden());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
    
    @Get
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getAllPosts(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String userId,
            @Nullable @QueryValue String wall,
            @Nullable @QueryValue Boolean hidden,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder,
            HttpRequest<?> request) {
        
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        Pageable pageable = Pageable.from(page - 1, limit);
        UUID userIdUuid = userId != null ? UUID.fromString(userId) : null;
        Page<Post> postsPage = adminPostService.getAllPosts(pageable, userIdUuid, wall, hidden, sortBy, sortOrder);
        
        List<AdminPostDTO> postDTOs = postsPage.getContent().stream()
                .map(this::mapPostToDTO)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", postDTOs);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", postsPage.getTotalSize());
        pagination.put("totalPages", postsPage.getTotalPages());
        response.put("pagination", pagination);
        
        return HttpResponse.ok(response);
    }
    
    @Get("/{id}")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<AdminPostDTO> getPostById(@PathVariable String id) {
        UUID postId = UUID.fromString(id);
        Post post = adminPostService.getPostById(postId);
        return HttpResponse.ok(mapPostToDTO(post));
    }
    
    @Put("/{id}/hide")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> hidePost(@PathVariable String id) {
        log.info("Admin hiding post: {}", id);
        UUID postId = UUID.fromString(id);
        adminPostService.hidePost(postId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Post hidden successfully");
        return HttpResponse.ok(response);
    }

    @Put("/{id}/unhide")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> unhidePost(@PathVariable String id) {
        log.info("Admin unhiding post: {}", id);
        UUID postId = UUID.fromString(id);
        adminPostService.unhidePost(postId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Post unhidden successfully");
        return HttpResponse.ok(response);
    }

    @Get("/{id}/comments")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getPostComments(
            @PathVariable String id,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit) {

        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;

        UUID postId = UUID.fromString(id);
        Pageable pageable = Pageable.from(page - 1, limit);
        Page<Comment> commentsPage = adminPostService.getPostComments(postId, pageable);

        List<AdminCommentDTO> commentDTOs = commentsPage.getContent().stream()
                .map(this::mapCommentToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", commentDTOs);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", commentsPage.getTotalSize());
        pagination.put("totalPages", commentsPage.getTotalPages());
        response.put("pagination", pagination);

        return HttpResponse.ok(response);
    }

    @Get("/{id}/images")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getPostImages(@PathVariable String id) {
        UUID postId = UUID.fromString(id);
        List<String> imageUrls = adminPostService.getPostImages(postId);
        Map<String, Object> response = new HashMap<>();
        response.put("postId", postId);
        response.put("imageUrls", imageUrls);
        return HttpResponse.ok(response);
    }

    @Get("/{id}/poll")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getPostPoll(@PathVariable String id) {
        UUID postId = UUID.fromString(id);
        Map<String, Object> pollData = adminPostService.getPollData(postId);
        return HttpResponse.ok(pollData);
    }
    
    @Get("/by-wall")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getPostsByWall(
            @Nullable @QueryValue String wall,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String sortBy,
            HttpRequest<?> request) {
        
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;
        
        Pageable pageable = Pageable.from(page - 1, limit);
        SortBy sort = SortBy.parse(sortBy);
        Page<Post> postsPage = adminPostService.getPostsByWall(wall, pageable, sort);
        
        List<AdminPostDTO> postDTOs = postsPage.getContent().stream()
                .map(this::mapPostToDTO)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", postDTOs);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", postsPage.getTotalSize());
        pagination.put("totalPages", postsPage.getTotalPages());
        response.put("pagination", pagination);
        
        return HttpResponse.ok(response);
    }
}
