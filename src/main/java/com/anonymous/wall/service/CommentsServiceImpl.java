package com.anonymous.wall.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class CommentsServiceImpl implements CommentsService {

    private static final Logger log = LoggerFactory.getLogger(CommentsServiceImpl.class);

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private PostRepository postRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private com.anonymous.wall.repository.CommentReportRepository commentReportRepository;

    /**
     * Add a comment to a post
     * For campus posts: only users from the same school can comment
     * For national posts: all authenticated users can comment
     * Uses atomic operations to increment comment count
     */
    @Override
    @Transactional
    @Retryable(attempts = "3", delay = "500ms")
    public Comment addComment(UUID postId, CreateCommentRequest request, UUID userId) {
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

        // Fetch user to get profile name
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        Comment comment = new Comment(postId, userId, request.getText());
        comment.setProfileName(userOpt.get().getProfileName());
        Comment savedComment = commentRepository.save(comment);

        // Atomically increment comment count on post
        post.incrementCommentCount();
        postRepository.update(post);

        log.info("Comment added: id={}, postId={}, user={}, newCommentCount={}",
            savedComment.getId(), postId, userId, post.getCommentCount());
        return savedComment;
    }

    /**
     * Get comments for a post with pagination and sorting
     */
    @Override
    public Page<Comment> getCommentsWithPagination(UUID postId, Pageable pageable, SortBy sortBy) {
        if (sortBy == null) {
            sortBy = SortBy.NEWEST; // Default sorting
        }

        log.debug("Fetching comments for post: {}, page: {}, limit: {}, sort: {}", postId, pageable.getNumber() + 1, pageable.getSize(), sortBy);

        // Comments only support sorting by created time
        Page<Comment> comments = switch (sortBy) {
            case NEWEST, MOST_LIKED -> commentRepository.findByPostIdAndHiddenFalseOrderByCreatedAtDesc(postId, pageable);
            case OLDEST, LEAST_LIKED -> commentRepository.findByPostIdAndHiddenFalseOrderByCreatedAtAsc(postId, pageable);
        };

        log.info("Retrieved {} comments for post: {}, sort: {}, total: {}", comments.getNumberOfElements(), postId, sortBy, comments.getTotalSize());
        return comments;
    }

    /**
     * Hide a comment (soft-delete)
     * Only the comment author can hide their own comment
     * Decrements the comment count on the post (soft-delete appears as deletion to user)
     */
    @Override
    @Transactional
    @Retryable(attempts = "3", delay = "500ms")
    public Comment hideComment(UUID postId, UUID commentId, UUID userId) {
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
    @Retryable(attempts = "3", delay = "500ms")
    public Comment unhideComment(UUID postId, UUID commentId, UUID userId) {
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
     * Get user's own comments with pagination and sorting
     * This is optimized with a composite index on (user_id, is_hidden, created_at)
     * Performs a single query instead of N+1 queries
     * Hidden comments are excluded (soft-deleted)
     */
    @Override
    public Page<Comment> getUserOwnComments(UUID userId, Pageable pageable, SortBy sortBy) {
        if (sortBy == null) {
            sortBy = SortBy.NEWEST; // Default sorting
        }

        log.debug("Fetching user's own comments: userId={}, page={}, limit={}, sort={}", 
            userId, pageable.getNumber() + 1, pageable.getSize(), sortBy);

        // Comments only support sorting by created time
        // MOST_LIKED/LEAST_LIKED are mapped to NEWEST/OLDEST for consistency with API
        // since comments don't have a like count field
        // Only non-hidden comments are returned
        Page<Comment> comments = switch (sortBy) {
            case NEWEST, MOST_LIKED -> commentRepository.findByUserIdAndHiddenFalseOrderByCreatedAtDesc(userId, pageable);
            case OLDEST, LEAST_LIKED -> commentRepository.findByUserIdAndHiddenFalseOrderByCreatedAtAsc(userId, pageable);
        };

        log.info("Retrieved {} comments for user: {}, sort: {}, total: {}", 
            comments.getNumberOfElements(), userId, sortBy, comments.getTotalSize());
        return comments;
    }

    @Override
    @Transactional
    public void reportComment(UUID commentId, UUID reporterUserId, String reason) {
        log.info("Reporting comment: commentId={}, reporterUserId={}, reason={}", commentId, reporterUserId, reason);

        // Check if comment exists
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new IllegalArgumentException("Comment not found");
        }

        Comment comment = commentOpt.get();

        // Check if user has already reported this comment
        if (commentReportRepository.existsByCommentIdAndReporterUserId(commentId, reporterUserId)) {
            throw new IllegalArgumentException("You have already reported this comment");
        }

        // Create the report
        com.anonymous.wall.entity.CommentReport report = new com.anonymous.wall.entity.CommentReport(commentId, reporterUserId, comment.getUserId(), reason);
        commentReportRepository.save(report);

        // Increment report count for the comment author
        Optional<UserEntity> authorOpt = userRepository.findById(comment.getUserId());
        if (authorOpt.isPresent()) {
            UserEntity author = authorOpt.get();
            author.setReportCount(author.getReportCount() + 1);
            userRepository.update(author);
            log.info("Incremented report count for user: userId={}, newCount={}", author.getId(), author.getReportCount());
        }

        log.info("Comment reported successfully: commentId={}, reporterUserId={}", commentId, reporterUserId);
    }
}
