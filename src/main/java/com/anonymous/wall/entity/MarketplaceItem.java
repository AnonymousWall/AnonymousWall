package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.DataType;
import io.micronaut.data.model.naming.NamingStrategies;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@MappedEntity(value = "marketplace_items", namingStrategy = NamingStrategies.Raw.class)
public class MarketplaceItem implements Commentable {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("profile_name")
    private String profileName = "Anonymous";

    @MappedProperty("title")
    private String title;

    @MappedProperty("description")
    private String description;

    @MappedProperty("price")
    private BigDecimal price;

    @MappedProperty("category")
    private String category;

    @MappedProperty("condition")
    private String condition;

    @MappedProperty("wall")
    private String wall = "campus";

    @MappedProperty("school_domain")
    private String schoolDomain;

    @MappedProperty("comment_count")
    private int commentCount = 0;

    @MappedProperty("is_hidden")
    private boolean hidden = false;

    @MappedProperty("image_urls")
    @TypeDef(type = DataType.JSON)
    private List<String> imageUrls = new ArrayList<>(); // Optional image URLs (up to 5)

    @Version
    @MappedProperty("version")
    private Long version = 0L;

    @MappedProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @MappedProperty("updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // ================= Constructors =================

    public MarketplaceItem() {
    }

    public MarketplaceItem(UUID userId, String title, String description, BigDecimal price, String category, String condition) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.condition = condition;
        this.profileName = "Anonymous";
        this.wall = "campus";
        this.imageUrls = new ArrayList<>();
        this.commentCount = 0;
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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getWall() { return wall; }
    public void setWall(String wall) { this.wall = wall; }

    public String getSchoolDomain() { return schoolDomain; }
    public void setSchoolDomain(String schoolDomain) { this.schoolDomain = schoolDomain; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>(); }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public void incrementCommentCount() { this.commentCount++; }

    @Override
    public void decrementCommentCount() {
        if (this.commentCount > 0) { this.commentCount--; }
    }
}
