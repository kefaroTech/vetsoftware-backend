package com.vetsoftware.app.baserolepermission.domain;

import java.time.LocalDateTime;

public class BaseRolePermission {
    private Long id;
    private BaseRoleRef baseRole;
    private BasePermissionRef basePermission;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public BaseRolePermission(Long id, BaseRoleRef baseRole, BasePermissionRef basePermission,
            LocalDateTime createdDate, boolean enabled) {
        if (baseRole == null)
            throw new IllegalArgumentException("baseRole is required");
        if (basePermission == null)
            throw new IllegalArgumentException("basePermission is required");
        this.id = id;
        this.baseRole = baseRole;
        this.basePermission = basePermission;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static BaseRolePermission create(BaseRoleRef baseRole,
            BasePermissionRef basePermission) {
        return new BaseRolePermission(null, baseRole, basePermission, LocalDateTime.now(), true);
    }

    public void update(BaseRoleRef baseRole, BasePermissionRef basePermission) {
        if (baseRole == null)
            throw new IllegalArgumentException("baseRole is required");
        if (basePermission == null)
            throw new IllegalArgumentException("basePermission is required");
        this.baseRole = baseRole;
        this.basePermission = basePermission;
    }

    public Long getId() {
        return id;
    }

    public BaseRoleRef getBaseRole() {
        return baseRole;
    }

    public BasePermissionRef getBasePermission() {
        return basePermission;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
