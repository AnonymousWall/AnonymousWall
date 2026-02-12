package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedEntity(value = "post_likes", namingStrategy = NamingStrategies.Raw.class)
public class PostLike {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("post_id")
    private UUID postId;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ================= Constructors =================

    public PostLike() {
    }

    public PostLike(UUID postId, UUID userId) {
        this.postId = postId;
        this.userId = userId;
        this.createdAt = OffsetDateTime.now();
    }

    // ================= Getters & Setters =================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
