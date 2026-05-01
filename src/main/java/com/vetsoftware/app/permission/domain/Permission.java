package com.vetsoftware.app.permission.domain;

import java.time.LocalDateTime;

public class Permission {
    private Long id;
    private String name;
    private String code;
    private Long companyId;
    private Long subModuleId;
    private final LocalDateTime createdDate;

    public Permission(Long id, String name, String code, Long companyId, Long subModuleId, LocalDateTime createdDate) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        if (companyId == null) throw new IllegalArgumentException("companyId is required");
        if (subModuleId == null) throw new IllegalArgumentException("subModuleId is required");
        this.id = id;
        this.name = name;
        this.code = code;
        this.companyId = companyId;
        this.subModuleId = subModuleId;
        this.createdDate = createdDate;
    }

    public static Permission create(String name, String code, Long companyId, Long subModuleId) {
        return new Permission(null, name, code, companyId, subModuleId, LocalDateTime.now());
    }

    public void update(String name, String code, Long companyId, Long subModuleId) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        if (companyId == null) throw new IllegalArgumentException("companyId is required");
        if (subModuleId == null) throw new IllegalArgumentException("subModuleId is required");
        this.name = name;
        this.code = code;
        this.companyId = companyId;
        this.subModuleId = subModuleId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public Long getCompanyId() { return companyId; }
    public Long getSubModuleId() { return subModuleId; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
