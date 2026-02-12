package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedEntity(value = "email_verification_codes", namingStrategy = NamingStrategies.Raw.class)
public class EmailVerificationCode {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("email")
    private String email;

    @MappedProperty("code")
    private String code;

    @MappedProperty("purpose")
    private String purpose; // register, login, reset_password

    @MappedProperty("expires_at")
    private OffsetDateTime expiresAt;

    @MappedProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // -------- Constructors --------
    public EmailVerificationCode() {}

    public EmailVerificationCode(String email, String code, String purpose, OffsetDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    // -------- Getters & Setters --------
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
