package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedEntity(value = "post_reports", namingStrategy = NamingStrategies.Raw.class)
public class PostReport {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("post_id")
    private UUID postId;

    @MappedProperty("reporter_user_id")
    private UUID reporterUserId;

    @MappedProperty("reason")
    private String reason;

    @MappedProperty("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    // ================= Constructors =================

    public PostReport() {
    }

    public PostReport(UUID postId, UUID reporterUserId, String reason) {
        this.postId = postId;
        this.reporterUserId = reporterUserId;
        this.reason = reason;
        this.createdAt = ZonedDateTime.now();
    }

    // ================= Getters & Setters =================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }

    public UUID getReporterUserId() { return reporterUserId; }
    public void setReporterUserId(UUID reporterUserId) { this.reporterUserId = reporterUserId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
