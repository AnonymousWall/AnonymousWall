package com.anonymous.wall.service;

import com.anonymous.wall.entity.*;
import com.anonymous.wall.model.CommentParentType;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.notification.event.CommentCreatedEvent;
import com.anonymous.wall.notification.event.InternshipCommentCreatedEvent;
import com.anonymous.wall.notification.event.MarketplaceCommentCreatedEvent;
import com.anonymous.wall.repository.*;
import io.micronaut.context.event.ApplicationEventPublisher;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class CommentsServiceImpl implements CommentsService {

    private static final Logger log = LoggerFactory.getLogger(CommentsServiceImpl.class);

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private PostRepository postRepository;

    @Inject
    private InternshipRepository internshipRepository;

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private CommentReportRepository commentReportRepository;

    @Inject
    private com.anonymous.wall.service.UserBlockService userBlockService;

    @Inject
    private ApplicationEventPublisher<CommentCreatedEvent> eventPublisher;

    @Inject
    private ApplicationEventPublisher<InternshipCommentCreatedEvent> internshipCommentEventPublisher;

    @Inject
    private ApplicationEventPublisher<MarketplaceCommentCreatedEvent> marketplaceCommentEventPublisher;

    /**
     * Resolve a Commentable entity by its parent type and ID.
     */
    private Commentable resolveParent(CommentParentType parentType, UUID parentId) {
        return switch (parentType) {
            case POST -> postRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Post not found"));
            case INTERNSHIP -> internshipRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Internship not found"));
            case MARKETPLACE -> marketplaceItemRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Marketplace item not found"));
        };
    }

    /**
     * Save the updated parent entity back to its repository after comment count changes.
     */
    private void saveParent(CommentParentType parentType, Commentable parent) {
        switch (parentType) {
            case POST -> postRepository.update((Post) parent);
            case INTERNSHIP -> internshipRepository.update((Internship) parent);
            case MARKETPLACE -> marketplaceItemRepository.update((MarketplaceItem) parent);
        }
    }

    /**
     * Validate that a user has visibility/permission for a parent entity.
     * Campus entities: only visible/actionable by users from the same school.
     * National entities: visible/actionable by all users.
     */
    private void validateParentVisibility(Commentable parent, UUID userId) {
        if (parent.isHidden()) {
            throw new IllegalArgumentException("Content not found");
        }
        if ("national".equals(parent.getWall())) {
            log.debug("Validating national entity access for user: {}", userId);
            return;
        }
        if ("campus".equals(parent.getWall())) {
            log.debug("Validating campus entity access for user: {}, entitySchoolDomain: {}", userId, parent.getSchoolDomain());
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found during visibility check: {}", userId);
                throw new IllegalArgumentException("User not found");
            }
            UserEntity user = userOpt.get();
            String userSchoolDomain = user.getSchoolDomain();
            if (userSchoolDomain == null || userSchoolDomain.trim().isEmpty()) {
                log.warn("User has no school domain, cannot access campus content: {}", userId);
                throw new IllegalArgumentException("You do not have access to campus posts");
            }
            if (!userSchoolDomain.equals(parent.getSchoolDomain())) {
                log.warn("School domain mismatch - user: {}, userDomain: {}, entityDomain: {}", userId, userSchoolDomain, parent.getSchoolDomain());
                throw new IllegalArgumentException("You do not have access to posts from other schools");
            }
            log.debug("User validated for campus entity access: {}", userId);
        }
    }

    /**
     * Add a comment to a parent entity (post, internship, or marketplace item).
     * For campus entities: only users from the same school can comment.
     * For national entities: all authenticated users can comment.
     */
    @Override
    @Transactional
    @Retryable(attempts = "3", delay = "500ms")
    public Comment addComment(CommentParentType parentType, UUID parentId, CreateCommentRequest request, UUID userId) {
        Commentable parent = resolveParent(parentType, parentId);

        // Validate visibility and permission
        validateParentVisibility(parent, userId);

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

        Comment comment = new Comment(parentId, parentType.name(), userId, request.getText());
        comment.setProfileName(userOpt.get().getProfileName());
        Comment savedComment = commentRepository.save(comment);

        // Atomically increment comment count on parent
        parent.incrementCommentCount();
        saveParent(parentType, parent);

        // Publish event for push notifications
        if (parentType == CommentParentType.POST) {
            eventPublisher.publishEvent(new CommentCreatedEvent(
                    savedComment.getId(),
                    savedComment.getParentId(),
                    userId,
                    ((Post) parent).getUserId(),
                    ((Post) parent).getWall()
            ));
        } else if (parentType == CommentParentType.INTERNSHIP) {
            internshipCommentEventPublisher.publishEvent(new InternshipCommentCreatedEvent(
                    savedComment.getId(),
                    savedComment.getParentId(),
                    userId,
                    ((Internship) parent).getUserId()
            ));
        } else if (parentType == CommentParentType.MARKETPLACE) {
            marketplaceCommentEventPublisher.publishEvent(new MarketplaceCommentCreatedEvent(
                    savedComment.getId(),
                    savedComment.getParentId(),
                    userId,
                    ((MarketplaceItem) parent).getUserId()
            ));
        }

        log.info("Comment added: id={}, parentType={}, parentId={}, user={}, newCommentCount={}",
            savedComment.getId(), parentType, parentId, userId, parent.getCommentCount());
        return savedComment;
    }

    /**
     * Get comments for a parent entity with pagination and sorting.
     */
    @Override
    public Page<Comment> getCommentsWithPagination(CommentParentType parentType, UUID parentId, Pageable pageable, SortBy sortBy) {
        if (sortBy == null) {
            sortBy = SortBy.NEWEST;
        }

        String parentTypeStr = parentType.name();
        log.debug("Fetching comments for {}: {}, page: {}, limit: {}, sort: {}", parentType, parentId, pageable.getNumber() + 1, pageable.getSize(), sortBy);

        Page<Comment> comments = switch (sortBy) {
            case NEWEST, MOST_LIKED, MOST_COMMENTED -> commentRepository.findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtDesc(parentTypeStr, parentId, pageable);
            case OLDEST, LEAST_LIKED, LEAST_COMMENTED -> commentRepository.findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtAsc(parentTypeStr, parentId, pageable);
        };

        log.info("Retrieved {} comments for {} {}, sort: {}, total: {}", comments.getNumberOfElements(), parentType, parentId, sortBy, comments.getTotalSize());
        return comments;
    }

    /**
     * Get comments for a parent entity with pagination, sorting, and block filtering.
     */
    @Override
    public Page<Comment> getCommentsWithPagination(CommentParentType parentType, UUID parentId, Pageable pageable, SortBy sortBy, UUID currentUserId) {
        Page<Comment> comments = getCommentsWithPagination(parentType, parentId, pageable, sortBy);
        if (currentUserId == null) {
            return comments;
        }
        Set<UUID> blockedUserIds = userBlockService.getCombinedBlockedUserIds(currentUserId);
        if (blockedUserIds.isEmpty()) {
            return comments;
        }
        List<Comment> filtered = comments.getContent().stream()
                .filter(c -> !blockedUserIds.contains(c.getUserId()))
                .collect(Collectors.toList());
        long removed = comments.getContent().size() - filtered.size();
        return Page.of(filtered, comments.getPageable(), comments.getTotalSize() - removed);
    }

    /**
     * Hide a comment (soft-delete).
     * Only the comment author can hide their own comment.
     */
    @Override
    @Transactional
    @Retryable(attempts = "3", delay = "500ms")
    public Comment hideComment(CommentParentType parentType, UUID parentId, UUID commentId, UUID userId) {
        Commentable parent = resolveParent(parentType, parentId);

        // Validate visibility and permission
        validateParentVisibility(parent, userId);

        // Verify comment exists and belongs to this parent
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new IllegalArgumentException("Comment not found");
        }

        Comment comment = commentOpt.get();
        if (!comment.getParentId().equals(parentId)) {
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

        // Atomically decrement comment count on parent
        parent.decrementCommentCount();
        saveParent(parentType, parent);

        log.info("Comment hidden: id={}, parentType={}, parentId={}, user={}, newCommentCount={}",
            commentId, parentType, parentId, userId, parent.getCommentCount());
        return updatedComment;
    }

    /**
     * Unhide a comment (undo soft-delete).
     * Only the comment author can unhide their own comment.
     */
    @Override
    @Transactional
    @Retryable(attempts = "3", delay = "500ms")
    public Comment unhideComment(CommentParentType parentType, UUID parentId, UUID commentId, UUID userId) {
        Commentable parent = resolveParent(parentType, parentId);

        // Validate visibility and permission
        validateParentVisibility(parent, userId);

        // Verify comment exists and belongs to this parent
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new IllegalArgumentException("Comment not found");
        }

        Comment comment = commentOpt.get();
        if (!comment.getParentId().equals(parentId)) {
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

        // Atomically increment comment count on parent
        parent.incrementCommentCount();
        saveParent(parentType, parent);

        log.info("Comment unhidden: id={}, parentType={}, parentId={}, user={}, newCommentCount={}",
            commentId, parentType, parentId, userId, parent.getCommentCount());
        return updatedComment;
    }

    /**
     * Get user's own comments with pagination and sorting.
     */
    @Override
    public Page<Comment> getUserOwnComments(UUID userId, Pageable pageable, SortBy sortBy) {
        if (sortBy == null) {
            sortBy = SortBy.NEWEST;
        }

        log.debug("Fetching user's own comments: userId={}, page={}, limit={}, sort={}", 
            userId, pageable.getNumber() + 1, pageable.getSize(), sortBy);

        Page<Comment> comments = switch (sortBy) {
            case NEWEST, MOST_LIKED, MOST_COMMENTED -> commentRepository.findByUserIdAndHiddenFalseOrderByCreatedAtDesc(userId, pageable);
            case OLDEST, LEAST_LIKED, LEAST_COMMENTED -> commentRepository.findByUserIdAndHiddenFalseOrderByCreatedAtAsc(userId, pageable);
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
        CommentReport report = new CommentReport(commentId, reporterUserId, comment.getUserId(), reason);
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
