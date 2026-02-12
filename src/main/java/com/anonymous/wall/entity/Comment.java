package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedEntity(value = "comments", namingStrategy = NamingStrategies.Raw.class)
public class Comment {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("post_id")
    private UUID postId;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("profile_name")
    private String profileName = "Anonymous";

    @MappedProperty("text")
    private String text;

    @MappedProperty("is_hidden")
    private boolean hidden = false;

    @MappedProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Version
    @MappedProperty("version")
    private Long version = 0L; // Optimistic locking version

    // ================= Constructors =================

    public Comment() {
    }

    public Comment(UUID postId, UUID userId, String text) {
        this.postId = postId;
        this.userId = userId;
        this.text = text;
        this.profileName = "Anonymous";
        this.hidden = false;
        this.createdAt = OffsetDateTime.now();
        this.version = 0L;
    }

    // ================= Getters & Setters =================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName != null ? profileName : "Anonymous"; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
