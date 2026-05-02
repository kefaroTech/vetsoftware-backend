package com.vetsoftware.app.systempermission.domain;

import java.time.LocalDateTime;

public class SystemPermission {
    private Long id;
    private String name;
    private String code;
    private final LocalDateTime createdDate;

    public SystemPermission(Long id, String name, String code, LocalDateTime createdDate) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        this.id = id;
        this.name = name;
        this.code = code;
        this.createdDate = createdDate;
    }

    public static SystemPermission create(String name, String code) {
        return new SystemPermission(null, name, code, LocalDateTime.now());
    }

    public void update(String name, String code) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        this.name = name;
        this.code = code;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
