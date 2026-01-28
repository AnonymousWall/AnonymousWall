package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;

@MappedEntity(value = "email_verification_codes", namingStrategy = NamingStrategies.Raw.class)
public class EmailVerificationCode {

    @Id
    @AutoPopulated
    private Long id;

    @MappedProperty("email")
    private String email;

    @MappedProperty("code")
    private String code;

    @MappedProperty("purpose")
    private String purpose; // register, login, reset_password

    @MappedProperty("expires_at")
    private ZonedDateTime expiresAt;

    @MappedProperty("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    // -------- Constructors --------
    public EmailVerificationCode() {}

    public EmailVerificationCode(String email, String code, String purpose, ZonedDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    // -------- Getters & Setters --------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public ZonedDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(ZonedDateTime expiresAt) { this.expiresAt = expiresAt; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
