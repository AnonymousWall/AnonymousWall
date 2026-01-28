package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedEntity(value = "post_likes", namingStrategy = NamingStrategies.Raw.class)
public class PostLike {

    @Id
    @AutoPopulated
    private Long id;

    @MappedProperty("post_id")
    private Long postId;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    // ================= Constructors =================

    public PostLike() {
    }

    public PostLike(Long postId, UUID userId) {
        this.postId = postId;
        this.userId = userId;
        this.createdAt = ZonedDateTime.now();
    }

    // ================= Getters & Setters =================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
