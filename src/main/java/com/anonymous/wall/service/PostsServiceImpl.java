package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.PostLike;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
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
            // National posts are visible to all users
            posts = postRepository.findByWall("national", pageable);
        } else {
            // Campus posts: only visible to users from the same school
            String userSchoolDomain = currentUser.getSchoolDomain();
            if (userSchoolDomain == null || userSchoolDomain.trim().isEmpty()) {
                // User has no school domain, cannot see campus posts
                posts = Page.empty();
            } else {
                posts = postRepository.findByWallAndSchoolDomain("campus", userSchoolDomain, pageable);
            }
        }

        // Enrich posts with like/comment counts and check if current user liked
        posts.getContent().forEach(post -> enrichPost(post, currentUserId));

        return posts;
    }

    /**
     * Add a comment to a post
     * For campus posts: only users from the same school can comment
     * For national posts: all authenticated users can comment
     */
    @Override
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

        Comment comment = new Comment(postId, userId, request.getText());
        Comment savedComment = commentRepository.save(comment);

        log.info("Comment added: id={}, postId={}, user={}", savedComment.getId(), postId, userId);
        return savedComment;
    }

    /**
     * Get all comments for a post
     */
    @Override
    public List<Comment> getComments(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    /**
     * Toggle like on a post
     * For campus posts: only users from the same school can like
     * For national posts: all authenticated users can like
     * Returns true if post is now liked, false if unliked
     */
    @Override
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
            // Unlike
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            log.info("Post unliked: postId={}, user={}", postId, userId);
            return false;
        } else {
            // Like
            PostLike like = new PostLike(postId, userId);
            postLikeRepository.save(like);
            log.info("Post liked: postId={}, user={}", postId, userId);
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
     * Enrich post with like/comment counts and check if current user liked
     */
    private void enrichPost(Post post, UUID currentUserId) {
        // Get like count
        long likeCount = postLikeRepository.countByPostId(post.getId());
        post.setLikeCount((int) likeCount);

        // Get comment count
        long commentCount = commentRepository.countByPostId(post.getId());
        post.setCommentCount((int) commentCount);

        // Check if current user liked this post
        if (currentUserId != null) {
            Optional<PostLike> userLike = postLikeRepository.findByPostIdAndUserId(post.getId(), currentUserId);
            post.setLiked(userLike.isPresent());
        }
    }
}
