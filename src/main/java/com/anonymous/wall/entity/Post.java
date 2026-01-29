package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedEntity(value = "posts", namingStrategy = NamingStrategies.Raw.class)
public class Post {

    @Id
    @GeneratedValue
    private Long id;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("content")
    private String content;

    @MappedProperty("wall")
    private String wall = "campus"; // "campus" or "national"

    @MappedProperty("school_domain")
    private String schoolDomain; // School domain for campus posts

    @MappedProperty("like_count")
    private int likeCount = 0; // Atomic counter for likes

    @MappedProperty("comment_count")
    private int commentCount = 0; // Atomic counter for comments

    @MappedProperty("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @MappedProperty("updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @Version
    @MappedProperty("version")
    private Long version = 0L; // Optimistic locking version

    // Transient fields for DTOs (not persisted)
    @Transient
    private boolean liked = false; // Does current user like this post

    // ================= Constructors =================

    public Post() {
    }

    public Post(UUID userId, String content, String wall, String schoolDomain) {
        this.userId = userId;
        this.content = content;
        this.wall = wall != null ? wall : "campus";
        this.schoolDomain = schoolDomain;
        this.likeCount = 0;
        this.commentCount = 0;
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
        this.version = 0L;
    }

    // ================= Getters & Setters =================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getWall() { return wall; }
    public void setWall(String wall) { this.wall = wall; }

    public String getSchoolDomain() { return schoolDomain; }
    public void setSchoolDomain(String schoolDomain) { this.schoolDomain = schoolDomain; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    /**
     * Increment like count atomically
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * Decrement like count atomically
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * Increment comment count atomically
     */
    public void incrementCommentCount() {
        this.commentCount++;
    }

    /**
     * Decrement comment count atomically
     */
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }
}
