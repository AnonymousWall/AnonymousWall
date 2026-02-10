package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.PostLike;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.event.PostHiddenEvent;
import com.anonymous.wall.event.PostUnhiddenEvent;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class PostsServiceImpl implements PostsService {

    private static final Logger log = LoggerFactory.getLogger(PostsServiceImpl.class);

    @Inject
    private PostRepository postRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private PostLikeRepository postLikeRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private CommentsService commentsService;

    @Inject
    private com.anonymous.wall.repository.PostReportRepository postReportRepository;

    @Inject
    private ApplicationEventPublisher<PostHiddenEvent> postHiddenEventPublisher;

    @Inject
    private ApplicationEventPublisher<PostUnhiddenEvent> postUnhiddenEventPublisher;

    /**
     * Create a new post
     */
    @Override
    @Retryable(attempts = "3", delay = "500ms")
    public Post createPost(CreatePostRequest request, UUID userId) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Post title cannot be empty");
        }

        if (request.getTitle().length() > 255) {
            throw new IllegalArgumentException("Post title exceeds maximum length of 255 characters");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Post content cannot be empty");
        }

        if (request.getContent().length() > 5000) {
            throw new IllegalArgumentException("Post content exceeds maximum length of 5000 characters");
        }

        // Handle wall type - could be enum or string
        String wall = "campus";
        if (request.getWall() != null) {
            wall = request.getWall().toString().toLowerCase();
        }

        if (!wall.equals("campus") && !wall.equals("national")) {
            throw new IllegalArgumentException("Wall must be 'campus' or 'national'");
        }

        // Fetch user's school domain
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        UserEntity user = userOpt.get();
        String schoolDomain = null;

        // For campus posts, school_domain must be set
        if (wall.equals("campus")) {
            schoolDomain = user.getSchoolDomain();
            if (schoolDomain == null || schoolDomain.trim().isEmpty()) {
                throw new IllegalArgumentException("Cannot post to campus wall without school domain");
            }
        }

        Post post = new Post(userId, request.getTitle(), request.getContent(), wall, schoolDomain);
        post.setProfileName(user.getProfileName());
        Post savedPost = postRepository.save(post);

        log.info("Post created: id={}, wall={}, schoolDomain={}, user={}", savedPost.getId(), wall, schoolDomain, userId);
        return savedPost;
    }

    /**
     * Get posts by wall type with pagination
     * Campus posts: only visible to users with the same school domain
     * National posts: visible to all users
     */
    @Override
    public Page<Post> getPostsByWall(String wall, Pageable pageable, UUID currentUserId) {
        if (!wall.equals("campus") && !wall.equals("national")) {
            throw new IllegalArgumentException("Wall must be 'campus' or 'national'");
        }

        log.debug("Fetching posts for wall: {}, page: {}, limit: {}, user: {}", wall, pageable.getNumber() + 1, pageable.getSize(), currentUserId);

        // Fetch current user to get their school domain
        Optional<UserEntity> userOpt = userRepository.findById(currentUserId);
        if (userOpt.isEmpty()) {
            log.warn("User not found when fetching posts: {}", currentUserId);
            throw new IllegalArgumentException("User not found");
        }

        UserEntity currentUser = userOpt.get();
        Page<Post> posts;

        if (wall.equals("national")) {
            // National posts are visible to all users (default sort by newest), excluding hidden posts
            posts = postRepository.findByWallAndHiddenFalseOrderByCreatedAtDesc("national", pageable);
            log.debug("Retrieved {} national posts for user: {}", posts.getNumberOfElements(), currentUserId);
        } else {
            // Campus posts: only visible to users from the same school, excluding hidden posts
            String userSchoolDomain = currentUser.getSchoolDomain();
            if (userSchoolDomain == null || userSchoolDomain.trim().isEmpty()) {
                // User has no school domain, cannot see campus posts
                log.warn("User has no school domain, cannot retrieve campus posts: {}", currentUserId);
                posts = Page.empty();
            } else {
                posts = postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc("campus", userSchoolDomain, pageable);
                log.debug("Retrieved {} campus posts for user: {}, schoolDomain: {}", posts.getNumberOfElements(), currentUserId, userSchoolDomain);
            }
        }

        // Enrich posts with like/comment counts and check if current user liked (batch operation)
        enrichPosts(posts.getContent(), currentUserId);

        log.info("Posts retrieved: wall={}, count={}, user={}", wall, posts.getNumberOfElements(), currentUserId);
        return posts;
    }

    /**
     * Get posts by wall type with pagination (optimized - schoolDomain from JWT)
     * Campus posts: only visible to users with the same school domain
     * National posts: visible to all users
     * This method avoids redundant user lookup by using schoolDomain from JWT claims
     */
    @Override
    public Page<Post> getPostsByWall(String wall, Pageable pageable, UUID currentUserId, String schoolDomain) {
        if (!wall.equals("campus") && !wall.equals("national")) {
            throw new IllegalArgumentException("Wall must be 'campus' or 'national'");
        }

        log.debug("Fetching posts for wall: {}, page: {}, limit: {}, user: {}", wall, pageable.getNumber() + 1, pageable.getSize(), currentUserId);

        Page<Post> posts;

        if (wall.equals("national")) {
            // National posts are visible to all users (default sort by newest), excluding hidden posts
            posts = postRepository.findByWallAndHiddenFalseOrderByCreatedAtDesc("national", pageable);
            log.debug("Retrieved {} national posts for user: {}", posts.getNumberOfElements(), currentUserId);
        } else {
            // Campus posts: only visible to users from the same school, excluding hidden posts
            if (schoolDomain == null || schoolDomain.trim().isEmpty()) {
                // User has no school domain, cannot see campus posts
                log.warn("User has no school domain, cannot retrieve campus posts: {}", currentUserId);
                posts = Page.empty();
            } else {
                posts = postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc("campus", schoolDomain, pageable);
                log.debug("Retrieved {} campus posts for user: {}, schoolDomain: {}", posts.getNumberOfElements(), currentUserId, schoolDomain);
            }
        }

        // Enrich posts with like/comment counts and check if current user liked (batch operation)
        enrichPosts(posts.getContent(), currentUserId);

        log.info("Posts retrieved: wall={}, count={}, user={}", wall, posts.getNumberOfElements(), currentUserId);
        return posts;
    }

    /**
     * Get posts by wall type with pagination and sorting
     * Campus posts: only visible to users with the same school domain
     * National posts: visible to all users
     */
    @Override
    public Page<Post> getPostsByWall(String wall, Pageable pageable, UUID currentUserId, SortBy sortBy) {
        if (!wall.equals("campus") && !wall.equals("national")) {
            throw new IllegalArgumentException("Wall must be 'campus' or 'national'");
        }

        if (sortBy == null) {
            sortBy = SortBy.NEWEST; // Default sorting
        }

        log.debug("Fetching posts for wall: {}, page: {}, limit: {}, sort: {}, user: {}", wall, pageable.getNumber() + 1, pageable.getSize(), sortBy, currentUserId);

        // Fetch current user to get their school domain
        Optional<UserEntity> userOpt = userRepository.findById(currentUserId);
        if (userOpt.isEmpty()) {
            log.warn("User not found when fetching posts with sort: {}", currentUserId);
            throw new IllegalArgumentException("User not found");
        }

        UserEntity currentUser = userOpt.get();
        Page<Post> posts;

        if (wall.equals("national")) {
            // National posts are visible to all users
            posts = getPostsWithSort("national", null, pageable, sortBy);
            log.debug("Retrieved {} national posts (sort: {}) for user: {}", posts.getNumberOfElements(), sortBy, currentUserId);
        } else {
            // Campus posts: only visible to users from the same school
            String userSchoolDomain = currentUser.getSchoolDomain();
            if (userSchoolDomain == null || userSchoolDomain.trim().isEmpty()) {
                // User has no school domain, cannot see campus posts
                log.warn("User has no school domain, cannot retrieve campus posts with sort: {}", currentUserId);
                posts = Page.empty();
            } else {
                posts = getPostsWithSort("campus", userSchoolDomain, pageable, sortBy);
                log.debug("Retrieved {} campus posts (sort: {}) for user: {}, schoolDomain: {}", posts.getNumberOfElements(), sortBy, currentUserId, userSchoolDomain);
            }
        }

        // Enrich posts with like/comment counts and check if current user liked (batch operation)
        enrichPosts(posts.getContent(), currentUserId);

        log.info("Posts retrieved: wall={}, sort={}, count={}, user={}", wall, sortBy, posts.getNumberOfElements(), currentUserId);
        return posts;
    }

    /**
     * Get posts by wall type with pagination and sorting (optimized - schoolDomain from JWT)
     * Campus posts: only visible to users with the same school domain
     * National posts: visible to all users
     * This method avoids redundant user lookup by using schoolDomain from JWT claims
     */
    @Override
    public Page<Post> getPostsByWall(String wall, Pageable pageable, UUID currentUserId, String schoolDomain, SortBy sortBy) {
        if (!wall.equals("campus") && !wall.equals("national")) {
            throw new IllegalArgumentException("Wall must be 'campus' or 'national'");
        }

        if (sortBy == null) {
            sortBy = SortBy.NEWEST; // Default sorting
        }

        log.debug("Fetching posts for wall: {}, page: {}, limit: {}, sort: {}, user: {}", wall, pageable.getNumber() + 1, pageable.getSize(), sortBy, currentUserId);

        Page<Post> posts;

        if (wall.equals("national")) {
            // National posts are visible to all users
            posts = getPostsWithSort("national", null, pageable, sortBy);
            log.debug("Retrieved {} national posts (sort: {}) for user: {}", posts.getNumberOfElements(), sortBy, currentUserId);
        } else {
            // Campus posts: only visible to users from the same school
            if (schoolDomain == null || schoolDomain.trim().isEmpty()) {
                // User has no school domain, cannot see campus posts
                log.warn("User has no school domain, cannot retrieve campus posts with sort: {}", currentUserId);
                posts = Page.empty();
            } else {
                posts = getPostsWithSort("campus", schoolDomain, pageable, sortBy);
                log.debug("Retrieved {} campus posts (sort: {}) for user: {}, schoolDomain: {}", posts.getNumberOfElements(), sortBy, currentUserId, schoolDomain);
            }
        }

        // Enrich posts with like/comment counts and check if current user liked (batch operation)
        enrichPosts(posts.getContent(), currentUserId);

        log.info("Posts retrieved: wall={}, sort={}, count={}, user={}", wall, sortBy, posts.getNumberOfElements(), currentUserId);
        return posts;
    }

    /**
     * Helper method to get posts with specified sorting
     */
    private Page<Post> getPostsWithSort(String wall, String schoolDomain, Pageable pageable, SortBy sortBy) {
        if (schoolDomain == null) {
            // National posts (filter hidden)
            return switch (sortBy) {
                case NEWEST -> postRepository.findByWallAndHiddenFalseOrderByCreatedAtDesc(wall, pageable);
                case OLDEST -> postRepository.findByWallAndHiddenFalseOrderByCreatedAtAsc(wall, pageable);
                case MOST_LIKED -> postRepository.findByWallAndHiddenFalseOrderByLikeCountDesc(wall, pageable);
                case LEAST_LIKED -> postRepository.findByWallAndHiddenFalseOrderByLikeCountAsc(wall, pageable);
            };
        } else {
            // Campus posts (filter hidden)
            return switch (sortBy) {
                case NEWEST -> postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc(wall, schoolDomain, pageable);
                case OLDEST -> postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtAsc(wall, schoolDomain, pageable);
                case MOST_LIKED -> postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByLikeCountDesc(wall, schoolDomain, pageable);
                case LEAST_LIKED -> postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByLikeCountAsc(wall, schoolDomain, pageable);
            };
        }
    }

    /**
     * Toggle like on a post and return detailed information
     * For campus posts: only users from the same school can like
     * For national posts: all authenticated users can like
     * Returns a map with:
     * - "liked": boolean indicating if post is now liked
     * - "likeCount": long indicating the total number of likes
     * Uses atomic operations to prevent race conditions
     * All changes (increment/decrement + like record) happen in the same transaction
     */
    @Override
    @Transactional
    @Retryable(attempts = "5", delay = "100ms")
    public Map<String, Object> toggleLikeWithDetails(UUID postId, UUID userId) {
        // Verify post exists
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();

        // Validate visibility and permission
        validatePostVisibility(post, userId);

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);
        boolean isNowLiked;

        if (existingLike.isPresent()) {
            // Unlike - decrement like count atomically
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            post.decrementLikeCount();
            postRepository.update(post);
            isNowLiked = false;
            log.info("Post unliked: postId={}, user={}, newLikeCount={}", postId, userId, post.getLikeCount());
        } else {
            // Like - increment like count atomically
            PostLike like = new PostLike(postId, userId);
            postLikeRepository.save(like);
            post.incrementLikeCount();
            postRepository.update(post);
            isNowLiked = true;
            log.info("Post liked: postId={}, user={}, newLikeCount={}", postId, userId, post.getLikeCount());
        }

        // Return both liked status and like count in a single response
        Map<String, Object> result = new HashMap<>();
        result.put("liked", isNowLiked);
        result.put("likeCount", (long) post.getLikeCount());

        return result;
    }

    /**
     * Get a single post with like/comment counts
     * Validates that the user has permission to view the post
     */
    @Override
    public Post getPost(UUID postId, UUID currentUserId) {
        log.debug("Fetching post: {}, user: {}", postId, currentUserId);

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            log.warn("Post not found: {}", postId);
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();
        validatePostVisibility(post, currentUserId);

        enrichPost(post, currentUserId);
        log.info("Post retrieved: {}, wall: {}, user: {}", postId, post.getWall(), currentUserId);
        return post;
    }

    /**
     * Validate that a user has visibility/permission for a post
     * Campus posts: only visible/actionable by users from the same school
     * National posts: visible/actionable by all users
     */
    private void validatePostVisibility(Post post, UUID userId) {
        if (post.getWall().equals("national")) {
            log.debug("Validating national post access for user: {}", userId);
            return;
        }
        if (post.getWall().equals("campus")) {
            log.debug("Validating campus post access for user: {}, postSchoolDomain: {}", userId, post.getSchoolDomain());
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found during visibility check: {}", userId);
                throw new IllegalArgumentException("User not found");
            }
            UserEntity user = userOpt.get();
            String userSchoolDomain = user.getSchoolDomain();
            if (userSchoolDomain == null || userSchoolDomain.trim().isEmpty()) {
                log.warn("User has no school domain, cannot access campus posts: {}", userId);
                throw new IllegalArgumentException("You do not have access to campus posts");
            }
            if (!userSchoolDomain.equals(post.getSchoolDomain())) {
                log.warn("School domain mismatch - user: {}, userDomain: {}, postDomain: {}", userId, userSchoolDomain, post.getSchoolDomain());
                throw new IllegalArgumentException("You do not have access to posts from other schools");
            }
            log.debug("User validated for campus post access: {}", userId);
        }
    }

    /**
     * Enrich post with current user's like status
     * Like and comment counts are now stored atomically in the database
     */
    private void enrichPost(Post post, UUID currentUserId) {
        // Like and comment counts are already set from database
        // No need to count - they're atomically maintained

        // Check if current user liked this post
        if (currentUserId != null) {
            Optional<PostLike> userLike = postLikeRepository.findByPostIdAndUserId(post.getId(), currentUserId);
            post.setLiked(userLike.isPresent());
        }
    }

    /**
     * Batch enrich multiple posts with current user's like status
     * This method eliminates N+1 query problem by fetching all likes in a single query
     * Like and comment counts are already stored atomically in the database
     */
    private void enrichPosts(List<Post> posts, UUID currentUserId) {
        // Like and comment counts are already set from database
        // No need to count - they're atomically maintained

        // Check if current user liked any of these posts (batch query)
        if (currentUserId != null && !posts.isEmpty()) {
            // Collect all post IDs
            List<UUID> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());

            // Fetch all likes for these posts in a single query
            List<PostLike> userLikes = postLikeRepository.findByUserIdAndPostIdIn(currentUserId, postIds);
            
            // Create a set of liked post IDs for O(1) lookup
            Set<UUID> likedPostIds = userLikes.stream()
                .map(PostLike::getPostId)
                .collect(Collectors.toSet());

            // Enrich all posts with like status
            posts.forEach(post -> post.setLiked(likedPostIds.contains(post.getId())));
        }
    }

    /**
     * Hide a post (soft-delete)
     * Only the post author can hide their own post
     * When a post is hidden, all its comments are also hidden asynchronously via event
     */
    @Override
    @Transactional
    @Retryable(attempts = "3", delay = "500ms")
    public Post hidePost(UUID postId, UUID userId) {
        // Verify post exists
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();

        // Only the post author can hide their own post
        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only hide your own posts");
        }

        // If already hidden, just return
        if (post.isHidden()) {
            return post;
        }

        // Hide the post
        post.setHidden(true);
        Post updatedPost = postRepository.update(post);

        // Publish event for async comment hiding
        postHiddenEventPublisher.publishEvent(new PostHiddenEvent(postId, userId));
        log.debug("Published PostHiddenEvent for postId={}", postId);

        log.info("Post hidden: id={}, user={}", postId, userId);
        return updatedPost;
    }

    /**
     * Unhide a post (undo soft-delete)
     * Only the post author can unhide their own post
     * When a post is unhidden, all its comments are restored asynchronously via event
     */
    @Override
    @Transactional
    @Retryable(attempts = "3", delay = "500ms")
    public Post unhidePost(UUID postId, UUID userId) {
        // Verify post exists
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();

        // Only the post author can unhide their own post
        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only unhide your own posts");
        }

        // If not hidden, just return
        if (!post.isHidden()) {
            return post;
        }

        // Unhide the post
        post.setHidden(false);
        Post updatedPost = postRepository.update(post);

        // Publish event for async comment unhiding
        postUnhiddenEventPublisher.publishEvent(new PostUnhiddenEvent(postId, userId));
        log.debug("Published PostUnhiddenEvent for postId={}", postId);

        log.info("Post unhidden: id={}, user={}", postId, userId);
        return updatedPost;
    }

    @Override
    public Page<Post> getUserOwnPosts(UUID userId, Pageable pageable, SortBy sortBy) {
        if (sortBy == null) {
            sortBy = SortBy.NEWEST; // Default sorting
        }

        log.debug("Fetching user's own posts: userId={}, page={}, limit={}, sort={}", 
            userId, pageable.getNumber() + 1, pageable.getSize(), sortBy);

        // Only non-hidden posts are returned
        Page<Post> posts = switch (sortBy) {
            case NEWEST -> postRepository.findByUserIdAndHiddenFalseOrderByCreatedAtDesc(userId, pageable);
            case OLDEST -> postRepository.findByUserIdAndHiddenFalseOrderByCreatedAtAsc(userId, pageable);
            case MOST_LIKED -> postRepository.findByUserIdAndHiddenFalseOrderByLikeCountDesc(userId, pageable);
            case LEAST_LIKED -> postRepository.findByUserIdAndHiddenFalseOrderByLikeCountAsc(userId, pageable);
        };

        log.info("Retrieved {} posts for user: {}, sort: {}, total: {}", 
            posts.getNumberOfElements(), userId, sortBy, posts.getTotalSize());
        return posts;
    }

    @Override
    @Transactional
    public void reportPost(UUID postId, UUID reporterUserId, String reason) {
        log.info("Reporting post: postId={}, reporterUserId={}, reason={}", postId, reporterUserId, reason);

        // Check if post exists
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();

        // Check if user has already reported this post
        if (postReportRepository.existsByPostIdAndReporterUserId(postId, reporterUserId)) {
            throw new IllegalArgumentException("You have already reported this post");
        }

        // Create the report
        com.anonymous.wall.entity.PostReport report = new com.anonymous.wall.entity.PostReport(postId, reporterUserId, reason);
        postReportRepository.save(report);

        // Increment report count for the post author
        Optional<UserEntity> authorOpt = userRepository.findById(post.getUserId());
        if (authorOpt.isPresent()) {
            UserEntity author = authorOpt.get();
            author.setReportCount(author.getReportCount() + 1);
            userRepository.update(author);
            log.info("Incremented report count for user: userId={}, newCount={}", author.getId(), author.getReportCount());
        }

        log.info("Post reported successfully: postId={}, reporterUserId={}", postId, reporterUserId);
    }
}
