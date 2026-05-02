package com.vetsoftware.app.systemuser.domain;

import java.time.LocalDateTime;

public class SystemUser {
    private Long id;
    private String code;
    private String hashPassword;
    private final LocalDateTime createdDate;

    public SystemUser(Long id, String code, String hashPassword, LocalDateTime createdDate) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        if (hashPassword == null || hashPassword.isBlank()) throw new IllegalArgumentException("password is required");
        this.id = id;
        this.code = code;
        this.hashPassword = hashPassword;
        this.createdDate = createdDate;
    }

    public static SystemUser create(String code, String hashPassword) {
        return new SystemUser(null, code, hashPassword, LocalDateTime.now());
    }

    public void update(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        this.code = code;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getHashPassword() { return hashPassword; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
