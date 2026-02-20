package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.CommentRepository;
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

    @Inject
    private CommentRepository commentRepository;
    
    /**
     * Get all posts with pagination and optional filters/sorting.
     * 
     * @param pageable Pagination parameters
     * @param userId Filter by author user ID (null = all authors)
     * @param hidden Filter by hidden status (null = all posts)
     * @param sortBy Sort field (case-insensitive): "createdAt", "likeCount", "commentCount", "userId"
     * @param sortOrder Sort order (case-insensitive): "asc" or "desc" (default: desc)
     * @return Page of posts matching the criteria
     */
    @Override
    public Page<Post> getAllPosts(Pageable pageable, UUID userId, Boolean hidden, String sortBy, String sortOrder) {
        log.info("Admin fetching posts - userId={}, hidden={}, sortBy={}, sortOrder={}", 
                 userId, hidden, sortBy, sortOrder);
        
        // Determine sort order (default to desc)
        boolean isDesc = sortOrder == null || sortOrder.equalsIgnoreCase("desc");
        
        // Case 1: No filters, no custom sorting - return all with default pagination
        // Note: Without explicit ORDER BY, the result order is database-dependent and not guaranteed
        if (userId == null && hidden == null && sortBy == null) {
            return postRepository.findAll(pageable);
        }
        
        // Case 2: No filters, but custom sorting specified - use sorting methods
        if (userId == null && hidden == null && sortBy != null) {
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
                
                case "userid":
                case "author":
                    return isDesc ?
                        postRepository.findAllOrderByUserIdDesc(pageable) :
                        postRepository.findAllOrderByUserIdAsc(pageable);
                
                default:
                    return postRepository.findAll(pageable);
            }
        }
        
        // Case 3: Filters specified (with or without sorting)
        // Note: Micronaut Data filter methods (findByHidden, findByUserId, findByUserIdAndHidden)
        // don't support dynamic sorting. Without explicit ORDER BY, result order is database-dependent.
        // To add custom sorting with filters, we would need repository methods like:
        // findByHiddenOrderByCreatedAtDesc, findByUserIdOrderByLikeCountDesc, etc.
        
        if (sortBy != null) {
            log.warn("sortBy parameter '{}' is not supported with filter parameters (userId={}, hidden={}) and will be ignored",
                     sortBy, userId, hidden);
        }
        
        if (userId == null && hidden != null) {
            // Filter by hidden status only
            return postRepository.findByHidden(hidden, pageable);
        } else if (userId != null && hidden == null) {
            // Filter by userId only
            return postRepository.findByUserId(userId, pageable);
        }
        
        // Filter by both userId and hidden (both are non-null if we reach here)
        return postRepository.findByUserIdAndHidden(userId, hidden, pageable);
    }
    
    @Override
    public Post getPostById(UUID postId) {
        log.info("Admin fetching post by id: {}", postId);
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));
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
    
    /**
     * Get posts by wall with pagination and sorting (admin version)
     * Admin can see all posts (both hidden and non-hidden) without schoolDomain filtering
     * 
     * @param wall Wall type: "national", "campus", or null for all posts
     * @param pageable Pagination parameters
     * @param sortBy Sort type: NEWEST, OLDEST, MOST_LIKED, LEAST_LIKED
     * @return Page of posts matching the criteria
     */
    @Override
    public Page<Post> getPostsByWall(String wall, Pageable pageable, SortBy sortBy) {
        if (sortBy == null) {
            sortBy = SortBy.NEWEST; // Default sorting
        }
        
        log.info("Admin fetching posts by wall - wall={}, sortBy={}", wall, sortBy);
        
        // Validate wall parameter if provided
        if (wall != null && !wall.equals("national") && !wall.equals("campus")) {
            throw new IllegalArgumentException("Wall must be 'national', 'campus', or null");
        }
        
        // If wall is null, return all posts with sorting
        if (wall == null) {
            return switch (sortBy) {
                case NEWEST -> postRepository.findAllOrderByCreatedAtDesc(pageable);
                case OLDEST -> postRepository.findAllOrderByCreatedAtAsc(pageable);
                case MOST_LIKED -> postRepository.findAllOrderByLikeCountDesc(pageable);
                case LEAST_LIKED -> postRepository.findAllOrderByLikeCountAsc(pageable);
                case MOST_COMMENTED -> postRepository.findAllOrderByCommentCountDesc(pageable);
                case LEAST_COMMENTED -> postRepository.findAllOrderByCommentCountAsc(pageable);
            };
        }
        
        // Return posts filtered by wall type with sorting
        // Admin sees all posts (both hidden and non-hidden) without schoolDomain filter
        return switch (sortBy) {
            case NEWEST -> postRepository.findByWallOrderByCreatedAtDesc(wall, pageable);
            case OLDEST -> postRepository.findByWallOrderByCreatedAtAsc(wall, pageable);
            case MOST_LIKED -> postRepository.findByWallOrderByLikeCountDesc(wall, pageable);
            case LEAST_LIKED -> postRepository.findByWallOrderByLikeCountAsc(wall, pageable);
            case MOST_COMMENTED -> postRepository.findByWallOrderByCommentCountDesc(wall, pageable);
            case LEAST_COMMENTED -> postRepository.findByWallOrderByCommentCountAsc(wall, pageable);
        };
    }

    @Override
    public void hidePost(UUID postId) {
        log.info("Admin hiding post: {}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));
        post.setHidden(true);
        postRepository.update(post);
    }

    @Override
    public void unhidePost(UUID postId) {
        log.info("Admin unhiding post: {}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));
        post.setHidden(false);
        postRepository.update(post);
    }

    @Override
    public Page<Comment> getPostComments(UUID postId, Pageable pageable) {
        log.info("Admin fetching comments for post: {}", postId);
        return commentRepository.findByParentTypeAndParentId("POST", postId, pageable);
    }
}
