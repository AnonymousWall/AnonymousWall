package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedEntity(value = "posts", namingStrategy = NamingStrategies.Raw.class)
public class Post {

    @Id
    @AutoPopulated
    private Long id;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("content")
    private String content;

    @MappedProperty("wall")
    private String wall = "campus"; // "campus" or "national"

    @MappedProperty("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @MappedProperty("updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    // Transient fields for DTOs
    @Transient
    private int likeCount = 0;

    @Transient
    private int commentCount = 0;

    @Transient
    private boolean liked = false;

    // ================= Constructors =================

    public Post() {
    }

    public Post(UUID userId, String content, String wall) {
        this.userId = userId;
        this.content = content;
        this.wall = wall != null ? wall : "campus";
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
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
}
