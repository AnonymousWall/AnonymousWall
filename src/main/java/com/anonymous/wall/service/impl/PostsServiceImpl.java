package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.PostLike;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.event.PostHiddenEvent;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.service.base.*;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
    private static final int MAX_IMAGES_PER_POST = 5;

    @Inject
    private PostRepository postRepository;

    @Inject
    private Provider<CommentsService> commentsServiceProvider;

    @Inject
    private PostLikeService postLikeService;

    @Inject
    private UserService userService;

    @Inject
    private PostReportService postReportService;

    @Inject
    private ApplicationEventPublisher<PostHiddenEvent> postHiddenEventPublisher;

    @Inject
    private UserBlockService userBlockService;

    @Inject
    private Provider<PollService> pollServiceProvider;

    @Inject
    private PostsCache postsCache;

    /**
     * Create a new post with optional image uploads (up to 5 images).
     * Images are uploaded before the transaction begins; the post and any
     * poll options are then saved atomically so a poll post is never left
     * without its options.
     */
    @Override
    @Transactional
    @CacheInvalidate(cacheNames = "national-posts", all = true)
    public Post createPost(CreatePostRequest request, UUID userId) {
        // Validate image count before any DB access
        if (request.getImageObjectNames() != null && request.getImageObjectNames().size() > MAX_IMAGES_PER_POST) {
            throw new IllegalArgumentException("Maximum " + MAX_IMAGES_PER_POST + " images per post allowed");
        }

        String wall = validateAndResolveWall(request);
        UserEntity user = fetchUser(userId);
        String schoolDomain = resolveSchoolDomain(wall, user);

        List<String> imageUrls = request.getImageObjectNames() != null
                ? request.getImageObjectNames()
                : new ArrayList<>();

        Post post = new Post(userId, request.getTitle(), request.getContent(), wall, schoolDomain);
        post.setProfileName(user.getProfileName());
        post.setImageUrls(imageUrls);

        // Set post type if provided
        if (request.getPostType() != null) {
            post.setPostType(request.getPostType().getValue());
        }

        Post savedPost = postRepository.save(post);

        // Create poll options in the same transaction so that the post and its
        // options are committed atomically.  A poll post with no options is an
        // invalid state that must never be persisted.
        boolean isPoll = "poll".equals(savedPost.getPostType());
        if (isPoll && request.getPollOptions() != null && !request.getPollOptions().isEmpty()) {
            pollServiceProvider.get().createPollOptions(savedPost.getId(), request.getPollOptions());
        }

        log.info("Post created: id={}, wall={}, schoolDomain={}, user={}, imageCount={}", savedPost.getId(), wall, schoolDomain, userId, imageUrls.size());
        return savedPost;
    }

    /**
     * Validate request fields and resolve the wall value
     */
    private String validateAndResolveWall(CreatePostRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Post title cannot be empty");
        }

        if (request.getTitle().length() > 255) {
            throw new IllegalArgumentException("Post title exceeds maximum length of 255 characters");
        }

        // Content is required for standard posts; optional for poll posts
        boolean isPoll = request.getPostType() != null &&
                "poll".equals(request.getPostType().getValue());
        if (!isPoll) {
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Post content cannot be empty");
            }
        }

        if (request.getContent() != null && request.getContent().length() > 5000) {
            throw new IllegalArgumentException("Post content exceeds maximum length of 5000 characters");
        }

        String wall = "campus";
        if (request.getWall() != null) {
            wall = request.getWall().toString().toLowerCase();
        }

        if (!wall.equals("campus") && !wall.equals("national")) {
            throw new IllegalArgumentException("Wall must be 'campus' or 'national'");
        }

        return wall;
    }

    /**
     * Fetch user by ID, throwing if not found
     */
    private UserEntity fetchUser(UUID userId) {
        Optional<UserEntity> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        return userOpt.get();
    }

    /**
     * Resolve school domain for campus posts
     */
    private String resolveSchoolDomain(String wall, UserEntity user) {
        if (!wall.equals("campus")) {
            return null;
        }
        String schoolDomain = user.getSchoolDomain();
        if (schoolDomain == null || schoolDomain.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot post to campus wall without school domain");
        }
        return schoolDomain;
    }

    /**
     * Get posts by wall type with pagination and sorting
     * Campus posts: only visible to users with the same school domain
     * National posts: visible to all users
     */
    @Override
    @Transactional(readOnly = true)
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
            posts = getPostsWithSort(null, pageable, sortBy);
            log.debug("Retrieved {} national posts (sort: {}) for user: {}", posts.getNumberOfElements(), sortBy, currentUserId);
        } else {
            // Campus posts: only visible to users from the same school
            if (schoolDomain == null || schoolDomain.trim().isEmpty()) {
                // User has no school domain, cannot see campus posts
                log.warn("User has no school domain, cannot retrieve campus posts with sort: {}", currentUserId);
                posts = Page.empty();
            } else {
                posts = getPostsWithSort(schoolDomain, pageable, sortBy);
                log.debug("Retrieved {} campus posts (sort: {}) for user: {}, schoolDomain: {}", posts.getNumberOfElements(), sortBy, currentUserId, schoolDomain);
            }
        }

        // Filter out posts from users blocked in either direction.
        // Note: pagination total is approximated (subtracts filtered count from DB total);
        // for perfectly accurate counts, a DB-level NOT IN query would be required.
        Set<UUID> blockedUserIds = userBlockService.getCombinedBlockedUserIds(currentUserId);
        if (!blockedUserIds.isEmpty()) {
            List<Post> filteredContent = posts.getContent().stream()
                    .filter(p -> !blockedUserIds.contains(p.getUserId()))
                    .collect(Collectors.toList());
            long removed = posts.getContent().size() - filteredContent.size();
            posts = Page.of(filteredContent, posts.getPageable(), posts.getTotalSize() - removed);
        }

        // Enrich posts with like/comment counts and check if current user liked (batch operation)
        enrichPosts(posts.getContent(), currentUserId);

        log.info("Posts retrieved: wall={}, sort={}, count={}, user={}", wall, sortBy, posts.getNumberOfElements(), currentUserId);
        return posts;
    }

    /**
     * Helper method to get posts with specified sorting
     */
    private Page<Post> getPostsWithSort(String schoolDomain, Pageable pageable, SortBy sortBy) {
        // Both national (schoolDomain=null) and campus now go through the cache
        return postsCache.get(pageable.getNumber(), pageable.getSize(), sortBy, schoolDomain);
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
    public Map<String, Object> toggleLikeWithDetails(UUID postId, UUID userId) {
        // Verify post exists
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();

        // Validate visibility and permission
        validatePostVisibility(post, userId);

        Optional<PostLike> existingLike = postLikeService.findByPostIdAndUserId(postId, userId);
        boolean isNowLiked;

        if (existingLike.isPresent()) {
            // Unlike - decrement like count atomically
            postLikeService.deleteByPostIdAndUserId(postId, userId);
            post.decrementLikeCount();
            postRepository.update(post);
            isNowLiked = false;
            log.info("Post unliked: postId={}, user={}, newLikeCount={}", postId, userId, post.getLikeCount());
        } else {
            // Like - increment like count atomically
            PostLike like = new PostLike(postId, userId);
            postLikeService.save(like);
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
    @Transactional(readOnly = true)
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
        if (post.isHidden()) {
            throw new IllegalArgumentException("Post not found");
        }

        if (post.getWall().equals("national")) {
            log.debug("Validating national post access for user: {}", userId);
            return;
        }
        if (post.getWall().equals("campus")) {
            log.debug("Validating campus post access for user: {}, postSchoolDomain: {}", userId, post.getSchoolDomain());
            Optional<UserEntity> userOpt = userService.findById(userId);
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
            Optional<PostLike> userLike = postLikeService.findByPostIdAndUserId(post.getId(), currentUserId);
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
            List<PostLike> userLikes = postLikeService.findByUserIdAndPostIdIn(currentUserId, postIds);
            
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
    @CacheInvalidate(cacheNames = "national-posts", all = true)
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
        postsCache.invalidateAll();
        return updatedPost;
    }

    /**
     * Unhide a post (undo soft-delete)
     * Only the post author can unhide their own post
     * When a post is unhidden, all its comments are restored
     */
    @Override
    @Transactional
    @CacheInvalidate(cacheNames = "national-posts", all = true)
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

        // Unhide all comments associated with this post (within same transaction)
        commentsServiceProvider.get().updateByParentTypeAndParentId("POST", postId, false);

        log.info("Post unhidden: id={}, user={}", postId, userId);
        postsCache.invalidateAll();
        return updatedPost;
    }

    @Override
    @Transactional(readOnly = true)
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
            case MOST_COMMENTED -> postRepository.findByUserIdAndHiddenFalseOrderByCommentCountDesc(userId, pageable);
            case LEAST_COMMENTED -> postRepository.findByUserIdAndHiddenFalseOrderByCommentCountAsc(userId, pageable);
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
        if (postReportService.existsByPostIdAndReporterUserId(postId, reporterUserId)) {
            throw new IllegalArgumentException("You have already reported this post");
        }

        // Create the report
        com.anonymous.wall.entity.PostReport report = new com.anonymous.wall.entity.PostReport(postId, reporterUserId, post.getUserId(), reason);
        postReportService.save(report);

        // Increment report count for the post author
        Optional<UserEntity> authorOpt = userService.findById(post.getUserId());
        if (authorOpt.isPresent()) {
            UserEntity author = authorOpt.get();
            author.setReportCount(author.getReportCount() + 1);
            userService.update(author);
            log.info("Incremented report count for user: userId={}, newCount={}", author.getId(), author.getReportCount());
        }

        log.info("Post reported successfully: postId={}, reporterUserId={}", postId, reporterUserId);
    }

    @Override
    @Transactional(propagation = TransactionDefinition.Propagation.REQUIRES_NEW)
    public void updateProfileNameByUserId(UUID userId, String profileName) {
        try {
            log.info("Updating profile name for user: userId={}, newName={}", userId, profileName);
            postRepository.updateProfileNameByUserId(userId, profileName);
        } catch (Exception e) {
            log.warn("Attempt failed updating profile name: userId={}, error={}",
                    userId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findById(UUID postId) {
        return postRepository.findById(postId);
    }

    @Override
    @Transactional
    public void update(Post post) {
        postRepository.update(post);
    }

}
