package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.model.SortBy;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

/**
 * Service interface for admin post moderation operations
 */
public interface AdminPostService {
    
    Page<Post> getAllPosts(Pageable pageable, UUID userId, Boolean hidden, String sortBy, String sortOrder);
    
    Page<Post> getPostsByWall(String wall, Pageable pageable, SortBy sortBy);
    
    Post getPostById(UUID postId);
    
    void deletePost(UUID postId);

    void hidePost(UUID postId);

    void unhidePost(UUID postId);

    Page<Comment> getPostComments(UUID postId, Pageable pageable);
}
