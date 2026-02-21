package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
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

    @MappedProperty("reported_user_id")
    private UUID reportedUserId;

    @MappedProperty("reason")
    private String reason;

    @MappedProperty("status")
    private String status = "PENDING";

    @MappedProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ================= Constructors =================

    public PostReport() {
    }

    public PostReport(UUID postId, UUID reporterUserId, UUID reportedUserId, String reason) {
        this.postId = postId;
        this.reporterUserId = reporterUserId;
        this.reportedUserId = reportedUserId;
        this.reason = reason;
        this.createdAt = OffsetDateTime.now();
    }

    // ================= Getters & Setters =================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }

    public UUID getReporterUserId() { return reporterUserId; }
    public void setReporterUserId(UUID reporterUserId) { this.reporterUserId = reporterUserId; }

    public UUID getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(UUID reportedUserId) { this.reportedUserId = reportedUserId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
