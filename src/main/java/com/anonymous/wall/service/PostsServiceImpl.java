package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.PostLike;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostLikeRepository;
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

        Post post = new Post(userId, request.getContent(), wall);
        Post savedPost = postRepository.save(post);

        log.info("Post created: id={}, wall={}, user={}", savedPost.getId(), wall, userId);
        return savedPost;
    }

    /**
     * Get posts by wall type with pagination
     */
    @Override
    public Page<Post> getPostsByWall(String wall, Pageable pageable, UUID currentUserId) {
        if (!wall.equals("campus") && !wall.equals("national")) {
            throw new IllegalArgumentException("Wall must be 'campus' or 'national'");
        }

        Page<Post> posts = postRepository.findByWall(wall, pageable);

        // Enrich posts with like/comment counts and check if current user liked
        posts.getContent().forEach(post -> enrichPost(post, currentUserId));

        return posts;
    }

    /**
     * Add a comment to a post
     */
    @Override
    public Comment addComment(Long postId, CreateCommentRequest request, UUID userId) {
        // Verify post exists
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

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
     * Returns true if post is now liked, false if unliked
     */
    @Override
    public boolean toggleLike(Long postId, UUID userId) {
        // Verify post exists
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

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
     */
    @Override
    public Post getPost(Long postId, UUID currentUserId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found");
        }

        Post post = postOpt.get();
        enrichPost(post, currentUserId);
        return post;
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
