package com.vetsoftware.app.basepermission.domain;

import java.time.LocalDateTime;

public class BasePermission {
    private Long id;
    private String name;
    private String code;
    private Long subModuleId;
    private final LocalDateTime createdDate;

    public BasePermission(Long id, String name, String code, Long subModuleId, LocalDateTime createdDate) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        if (subModuleId == null) throw new IllegalArgumentException("subModuleId is required");
        this.id = id;
        this.name = name;
        this.code = code;
        this.subModuleId = subModuleId;
        this.createdDate = createdDate;
    }

    public static BasePermission create(String name, String code, Long subModuleId) {
        return new BasePermission(null, name, code, subModuleId, LocalDateTime.now());
    }

    public void update(String name, String code, Long subModuleId) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        if (subModuleId == null) throw new IllegalArgumentException("subModuleId is required");
        this.name = name;
        this.code = code;
        this.subModuleId = subModuleId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public Long getSubModuleId() { return subModuleId; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
