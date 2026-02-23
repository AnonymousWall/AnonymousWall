package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.DataType;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@MappedEntity(value = "posts", namingStrategy = NamingStrategies.Raw.class)
public class Post implements Commentable {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("profile_name")
    private String profileName = "Anonymous";

    @MappedProperty("title")
    private String title;

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

    @MappedProperty("is_hidden")
    private boolean hidden = false; // Soft-delete flag

    @MappedProperty("image_urls")
    @TypeDef(type = DataType.JSON)
    private List<String> imageUrls = new ArrayList<>(); // Optional image URLs for post (up to 5)

    @MappedProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @MappedProperty("updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Version
    @MappedProperty("version")
    private Long version = 0L; // Optimistic locking version

    // Transient fields for DTOs (not persisted)
    @Transient
    private boolean liked = false; // Does current user like this post

    // ================= Constructors =================

    public Post() {
    }

    public Post(UUID userId, String title, String content, String wall, String schoolDomain) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.wall = wall != null ? wall : "campus";
        this.schoolDomain = schoolDomain;
        this.profileName = "Anonymous";
        this.likeCount = 0;
        this.commentCount = 0;
        this.imageUrls = new ArrayList<>();
        this.hidden = false;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        this.version = 0L;
    }

    // ================= Getters & Setters =================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName != null ? profileName : "Anonymous"; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getWall() { return wall; }
    public void setWall(String wall) { this.wall = wall; }

    public String getSchoolDomain() { return schoolDomain; }
    public void setSchoolDomain(String schoolDomain) { this.schoolDomain = schoolDomain; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>(); }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public void addImageUrl(String url) {
        if (this.imageUrls == null) this.imageUrls = new ArrayList<>();
        this.imageUrls.add(url);
    }

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
