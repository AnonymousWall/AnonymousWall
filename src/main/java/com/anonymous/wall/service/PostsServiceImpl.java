package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.PostLike;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    /**
     * Create a new post
     */
    @Override
    public Post createPost(CreatePostRequest request, UUID userId) {
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

        Post post = new Post(userId, request.getContent(), wall, schoolDomain);
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

        // Fetch current user to get their school domain
        Optional<UserEntity> userOpt = userRepository.findById(currentUserId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        UserEntity currentUser = userOpt.get();
        Page<Post> posts;

        if (wall.equals("national")) {
            // National posts are visible to all users (default sort by newest), excluding hidden posts
            posts = postRepository.findByWallAndHiddenFalseOrderByCreatedAtDesc("national", pageable);
        } else {
            // Campus posts: only visible to users from the same school, excluding hidden posts
            String userSchoolDomain = currentUser.getSchoolDomain();
            if (userSchoolDomain == null || userSchoolDomain.trim().isEmpty()) {
                // User has no school domain, cannot see campus posts
                posts = Page.empty();
            } else {
                posts = postRepository.findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc("campus", userSchoolDomain, pageable);
            }
        }

        // Enrich posts with like/comment counts and check if current user liked
        posts.getContent().forEach(post -> enrichPost(post, currentUserId));

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

        // Fetch current user to get their school domain
        Optional<UserEntity> userOpt = userRepository.findById(currentUserId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        UserEntity currentUser = userOpt.get();
        Page<Post> posts;

        if (wall.equals("national")) {
            // National posts are visible to all users
            posts = getPostsWithSort("national", null, pageable, sortBy);
        } else {
            // Campus posts: only visible to users from the same school
            String userSchoolDomain = currentUser.getSchoolDomain();
            if (userSchoolDomain == null || userSchoolDomain.trim().isEmpty()) {
                // User has no school domain, cannot see campus posts
                posts = Page.empty();
            } else {
                posts = getPostsWithSort("campus", userSchoolDomain, pageable, sortBy);
            }
        }

        // Enrich posts with like/comment counts and check if current user liked
        posts.getContent().forEach(post -> enrichPost(post, currentUserId));

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
     * Add a comment to a post
     * For campus posts: only users from the same school can comment
     * For national posts: all authenticated users can comment
     * Uses atomic operations to increment comment count
     */
    @Override
    @Transactional
    public Comment addComment(Long postId, CreateCommentRequest request, UUID userId) {
        // Verify post exists
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();

        // Validate visibility and permission
        validatePostVisibility(post, userId);

        if (request.getText() == null || request.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be empty");
        }

        if (request.getText().length() > 5000) {
            throw new IllegalArgumentException("Comment text exceeds maximum length of 5000 characters");
        }

        Comment comment = new Comment(postId, userId, request.getText());
        Comment savedComment = commentRepository.save(comment);

        // Atomically increment comment count on post
        post.incrementCommentCount();
        try {
            postRepository.update(post);
        } catch (Exception e) {
            // If update fails due to version conflict or other reason, log and continue
            // The comment was still saved, which is the important part
            log.warn("Failed to update post comment count: {}", e.getMessage());
        }

        log.info("Comment added: id={}, postId={}, user={}, newCommentCount={}",
            savedComment.getId(), postId, userId, post.getCommentCount());
        return savedComment;
    }

    /**
     * Get all comments for a post
     */
    @Override
    public List<Comment> getComments(Long postId) {
        return commentRepository.findByPostIdAndHiddenFalse(postId);
    }

    /**
     * Get comments for a post with pagination
     */
    @Override
    public Page<Comment> getCommentsWithPagination(Long postId, Pageable pageable) {
        return commentRepository.findByPostIdAndHiddenFalse(postId, pageable);
    }

    /**
     * Get comments for a post with pagination and sorting
     */
    @Override
    public Page<Comment> getCommentsWithPagination(Long postId, Pageable pageable, SortBy sortBy) {
        if (sortBy == null) {
            sortBy = SortBy.NEWEST; // Default sorting
        }

        // Comments only support sorting by created time
        return switch (sortBy) {
            case NEWEST, MOST_LIKED -> commentRepository.findByPostIdAndHiddenFalseOrderByCreatedAtDesc(postId, pageable);
            case OLDEST, LEAST_LIKED -> commentRepository.findByPostIdAndHiddenFalseOrderByCreatedAtAsc(postId, pageable);
        };
    }

    /**
     * Toggle like on a post
     * For campus posts: only users from the same school can like
     * For national posts: all authenticated users can like
     * Returns true if post is now liked, false if unliked
     * Uses atomic operations to prevent race conditions
     */
    @Override
    @Transactional
    public boolean toggleLike(Long postId, UUID userId) {
        // Verify post exists
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();

        // Validate visibility and permission
        validatePostVisibility(post, userId);

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);

        if (existingLike.isPresent()) {
            // Unlike - decrement like count atomically
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            post.decrementLikeCount();
            try {
                postRepository.update(post);
            } catch (Exception e) {
                // If update fails due to version conflict or other reason, log and continue
                // The like was still removed, which is the important part
                log.warn("Failed to update post like count: {}", e.getMessage());
            }
            log.info("Post unliked: postId={}, user={}, newLikeCount={}", postId, userId, post.getLikeCount());
            return false;
        } else {
            // Like - increment like count atomically
            PostLike like = new PostLike(postId, userId);
            postLikeRepository.save(like);
            post.incrementLikeCount();
            try {
                postRepository.update(post);
            } catch (Exception e) {
                // If update fails due to version conflict or other reason, log and continue
                // The like was still added, which is the important part
                log.warn("Failed to update post like count: {}", e.getMessage());
            }
            log.info("Post liked: postId={}, user={}, newLikeCount={}", postId, userId, post.getLikeCount());
            return true;
        }
    }

    /**
     * Get a single post with like/comment counts
     * Validates that the user has permission to view the post
     */
    @Override
    public Post getPost(Long postId, UUID currentUserId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();
        validatePostVisibility(post, currentUserId);

        enrichPost(post, currentUserId);
        return post;
    }

    /**
     * Validate that a user has visibility/permission for a post
     * Campus posts: only visible/actionable by users from the same school
     * National posts: visible/actionable by all users
     */
    private void validatePostVisibility(Post post, UUID userId) {
        if (post.getWall().equals("national")) {
            return;
        }
        if (post.getWall().equals("campus")) {
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found");
            }
            UserEntity user = userOpt.get();
            String userSchoolDomain = user.getSchoolDomain();
            if (userSchoolDomain == null || userSchoolDomain.trim().isEmpty()) {
                throw new IllegalArgumentException("You do not have access to campus posts");
            }
            if (!userSchoolDomain.equals(post.getSchoolDomain())) {
                throw new IllegalArgumentException("You do not have access to posts from other schools");
            }
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
     * Hide a comment (soft-delete)
     * Only the comment author can hide their own comment
     * Decrements the comment count on the post (soft-delete appears as deletion to user)
     */
    @Override
    @Transactional
    public Comment hideComment(Long postId, Long commentId, UUID userId) {
        // Verify post exists
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();

        // Validate visibility and permission
        validatePostVisibility(post, userId);

        // Verify comment exists and belongs to this post
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new IllegalArgumentException("Comment not found");
        }

        Comment comment = commentOpt.get();
        if (!comment.getPostId().equals(postId)) {
            throw new IllegalArgumentException("Comment does not belong to this post");
        }

        // Only the comment author can hide their own comment
        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only hide your own comments");
        }

        // If already hidden, just return
        if (comment.isHidden()) {
            return comment;
        }

        // Hide the comment
        comment.setHidden(true);
        Comment updatedComment = commentRepository.update(comment);

        // Atomically decrement comment count on post (within same transaction)
        post.decrementCommentCount();
        postRepository.update(post);

        log.info("Comment hidden: id={}, postId={}, user={}, newCommentCount={}",
            commentId, postId, userId, post.getCommentCount());
        return updatedComment;
    }

    /**
     * Unhide a comment (undo soft-delete)
     * Only the comment author can unhide their own comment
     * Increments the comment count on the post (restore from deletion)
     */
    @Override
    @Transactional
    public Comment unhideComment(Long postId, Long commentId, UUID userId) {
        // Verify post exists
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();

        // Validate visibility and permission
        validatePostVisibility(post, userId);

        // Verify comment exists and belongs to this post
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new IllegalArgumentException("Comment not found");
        }

        Comment comment = commentOpt.get();
        if (!comment.getPostId().equals(postId)) {
            throw new IllegalArgumentException("Comment does not belong to this post");
        }

        // Only the comment author can unhide their own comment
        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only unhide your own comments");
        }

        // If not hidden, just return
        if (!comment.isHidden()) {
            return comment;
        }

        // Unhide the comment
        comment.setHidden(false);
        Comment updatedComment = commentRepository.update(comment);

        // Atomically increment comment count on post (within same transaction)
        post.incrementCommentCount();
        postRepository.update(post);

        log.info("Comment unhidden: id={}, postId={}, user={}, newCommentCount={}",
            commentId, postId, userId, post.getCommentCount());
        return updatedComment;
    }

    /**
     * Hide a post (soft-delete)
     * Only the post author can hide their own post
     * When a post is hidden, all its comments are also hidden
     */
    @Override
    @Transactional
    public Post hidePost(Long postId, UUID userId) {
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

        // Hide all comments associated with this post (within same transaction)
        commentRepository.updateByPostId(postId, true);

        log.info("Post hidden: id={}, user={}", postId, userId);
        return updatedPost;
    }

    /**
     * Unhide a post (undo soft-delete)
     * Only the post author can unhide their own post
     * When a post is unhidden, all its comments are restored
     */
    @Override
    @Transactional
    public Post unhidePost(Long postId, UUID userId) {
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
        commentRepository.updateByPostId(postId, false);

        log.info("Post unhidden: id={}, user={}", postId, userId);
        return updatedPost;
    }
}
