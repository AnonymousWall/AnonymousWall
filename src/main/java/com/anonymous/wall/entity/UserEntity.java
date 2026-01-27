package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedEntity(value = "users", namingStrategy = NamingStrategies.Raw.class)
public class UserEntity {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("email")
    private String email;

    @MappedProperty("password_hash")
    private String passwordHash;

    @MappedProperty("is_verified")
    private boolean verified = false;

    @MappedProperty("password_set")
    private boolean passwordSet = false;

    @MappedProperty("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    // ---------------- Getters & Setters ----------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean new_verified) { verified = new_verified; }

    public boolean isPasswordSet() { return passwordSet; }
    public void setPasswordSet(boolean passwordSet) { this.passwordSet = passwordSet; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}