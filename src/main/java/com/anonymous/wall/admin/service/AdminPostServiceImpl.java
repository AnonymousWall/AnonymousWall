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
    
    /**
     * Get all posts with pagination and optional filters/sorting.
     * 
     * Note: When userId or hidden filters are active, custom sorting parameters are not applied.
     * For custom sorting, omit the filter parameters.
     * 
     * @param pageable Pagination parameters
     * @param userId Filter by author user ID (null = all authors)
     * @param hidden Filter by hidden status (null = all posts)
     * @param sortBy Sort field (case-insensitive): "createdAt", "likeCount", "commentCount", "reportCount", "userId"
     * @param sortOrder Sort order (case-insensitive): "asc" or "desc" (default: desc)
     * @return Page of posts matching the criteria
     */
    @Override
    public Page<Post> getAllPosts(Pageable pageable, UUID userId, Boolean hidden, String sortBy, String sortOrder) {
        log.info("Admin fetching posts - userId={}, hidden={}, sortBy={}, sortOrder={}", 
                 userId, hidden, sortBy, sortOrder);
        
        // Determine sort order (default to desc)
        boolean isDesc = sortOrder == null || sortOrder.equalsIgnoreCase("desc");
        
        // If userId or hidden filters are specified, use the existing filter methods
        // (Note: for simplicity, we don't combine filtering with custom sorting in this implementation)
        if (userId != null || hidden != null) {
            if (userId == null && hidden != null) {
                return postRepository.findByHidden(hidden, pageable);
            } else if (userId != null && hidden == null) {
                return postRepository.findByUserId(userId, pageable);
            } else {
                return postRepository.findByUserIdAndHidden(userId, hidden, pageable);
            }
        }
        
        // Handle sorting without filtering
        if (sortBy == null) {
            return postRepository.findAll(pageable);
        }
        
        switch (sortBy.toLowerCase()) {
            case "createdat":
                return isDesc ?
                    postRepository.findAllOrderByCreatedAtDesc(pageable) :
                    postRepository.findAllOrderByCreatedAtAsc(pageable);
            
            case "likecount":
                return isDesc ?
                    postRepository.findAllOrderByLikeCountDesc(pageable) :
                    postRepository.findAllOrderByLikeCountAsc(pageable);
            
            case "commentcount":
                return isDesc ?
                    postRepository.findAllOrderByCommentCountDesc(pageable) :
                    postRepository.findAllOrderByCommentCountAsc(pageable);
            
            case "reportcount":
                return isDesc ?
                    postRepository.findAllOrderByReportCountDesc(pageable) :
                    postRepository.findAllOrderByReportCountAsc(pageable);
            
            case "userid":
            case "author":
                return isDesc ?
                    postRepository.findAllOrderByUserIdDesc(pageable) :
                    postRepository.findAllOrderByUserIdAsc(pageable);
            
            default:
                return postRepository.findAll(pageable);
        }
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
