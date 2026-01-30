package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedEntity(value = "comments", namingStrategy = NamingStrategies.Raw.class)
public class Comment {

    @Id
    @GeneratedValue
    private Long id;

    @MappedProperty("post_id")
    private Long postId;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("text")
    private String text;

    @MappedProperty("is_hidden")
    private boolean hidden = false;

    @MappedProperty("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Version
    @MappedProperty("version")
    private Long version = 0L; // Optimistic locking version

    // ================= Constructors =================

    public Comment() {
    }

    public Comment(Long postId, UUID userId, String text) {
        this.postId = postId;
        this.userId = userId;
        this.text = text;
        this.hidden = false;
        this.createdAt = ZonedDateTime.now();
        this.version = 0L;
    }

    // ================= Getters & Setters =================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
