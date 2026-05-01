package com.vetsoftware.app.baserolepermission.domain;

import java.time.LocalDateTime;

public class BaseRolePermission {
    private Long id;
    private Long baseRoleId;
    private Long basePermissionId;
    private final LocalDateTime createdDate;

    public BaseRolePermission(Long id, Long baseRoleId, Long basePermissionId, LocalDateTime createdDate) {
        if (baseRoleId == null) throw new IllegalArgumentException("baseRoleId is required");
        if (basePermissionId == null) throw new IllegalArgumentException("basePermissionId is required");
        this.id = id;
        this.baseRoleId = baseRoleId;
        this.basePermissionId = basePermissionId;
        this.createdDate = createdDate;
    }

    public static BaseRolePermission create(Long baseRoleId, Long basePermissionId) {
        return new BaseRolePermission(null, baseRoleId, basePermissionId, LocalDateTime.now());
    }

    public void update(Long baseRoleId, Long basePermissionId) {
        if (baseRoleId == null) throw new IllegalArgumentException("baseRoleId is required");
        if (basePermissionId == null) throw new IllegalArgumentException("basePermissionId is required");
        this.baseRoleId = baseRoleId;
        this.basePermissionId = basePermissionId;
    }

    public Long getId() { return id; }
    public Long getBaseRoleId() { return baseRoleId; }
    public Long getBasePermissionId() { return basePermissionId; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
