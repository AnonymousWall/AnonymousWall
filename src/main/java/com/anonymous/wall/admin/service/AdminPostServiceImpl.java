package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.repository.PostRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Implementation of admin post moderation service
 */
@Singleton
public class AdminPostServiceImpl implements AdminPostService {
    
    private static final Logger log = LoggerFactory.getLogger(AdminPostServiceImpl.class);
    
    @Inject
    private PostRepository postRepository;
    
    @Override
    public Page<Post> getAllPosts(Pageable pageable, UUID userId, Boolean hidden) {
        log.info("Admin fetching posts with filters - userId: {}, hidden: {}", userId, hidden);
        
        // Note: Full filtering support requires additional repository methods
        // For now, return all posts. In future, can add methods like:
        // - findByUserId(UUID userId, Pageable pageable)
        // - findByHidden(boolean hidden, Pageable pageable) 
        // - findByUserIdAndHidden(UUID userId, boolean hidden, Pageable pageable)
        return postRepository.findAll(pageable);
    }
    
    @Override
    public void deletePost(UUID postId) {
        log.info("Admin soft-deleting post: {}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));
        
        post.setHidden(true);
        postRepository.update(post);
        log.info("Post soft-deleted successfully: {}", postId);
    }
}
