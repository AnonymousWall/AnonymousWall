package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedEntity(value = "users", namingStrategy = NamingStrategies.Raw.class)
public class UserEntity {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("email")
    private String email;

    @MappedProperty("profile_name")
    private String profileName = "Anonymous";

    @MappedProperty("school_domain")
    private String schoolDomain;

    @MappedProperty("password_hash")
    private String passwordHash;

    @MappedProperty("is_verified")
    private boolean verified = false;

    @MappedProperty("password_set")
    private boolean passwordSet = false;

    @MappedProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @MappedProperty("updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @MappedProperty("report_count")
    private int reportCount = 0;

    @MappedProperty("role")
    private String role = "USER";

    @MappedProperty("blocked")
    private boolean blocked = false;

    // ---------------- Getters & Setters ----------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName != null ? profileName : "Anonymous"; }

    public String getSchoolDomain() { return schoolDomain; }
    public void setSchoolDomain(String schoolDomain) { this.schoolDomain = schoolDomain; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean new_verified) { verified = new_verified; }

    public boolean isPasswordSet() { return passwordSet; }
    public void setPasswordSet(boolean passwordSet) { this.passwordSet = passwordSet; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
}