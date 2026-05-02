package com.vetsoftware.app.baserolepermission.domain;

import java.time.LocalDateTime;

public class BaseRolePermission {
    private Long id;
    private BaseRoleRef baseRole;
    private BasePermissionRef basePermission;
    private final LocalDateTime createdDate;

    public BaseRolePermission(Long id, BaseRoleRef baseRole, BasePermissionRef basePermission, LocalDateTime createdDate) {
        if (baseRole == null) throw new IllegalArgumentException("baseRole is required");
        if (basePermission == null) throw new IllegalArgumentException("basePermission is required");
        this.id = id;
        this.baseRole = baseRole;
        this.basePermission = basePermission;
        this.createdDate = createdDate;
    }

    public static BaseRolePermission create(BaseRoleRef baseRole, BasePermissionRef basePermission) {
        return new BaseRolePermission(null, baseRole, basePermission, LocalDateTime.now());
    }

    public void update(BaseRoleRef baseRole, BasePermissionRef basePermission) {
        if (baseRole == null) throw new IllegalArgumentException("baseRole is required");
        if (basePermission == null) throw new IllegalArgumentException("basePermission is required");
        this.baseRole = baseRole;
        this.basePermission = basePermission;
    }

    public Long getId() { return id; }
    public BaseRoleRef getBaseRole() { return baseRole; }
    public BasePermissionRef getBasePermission() { return basePermission; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
