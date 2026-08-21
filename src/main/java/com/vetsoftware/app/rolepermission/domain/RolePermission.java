package com.vetsoftware.app.rolepermission.domain;

import java.time.LocalDateTime;

public class RolePermission {
    private Long id;
    private RoleRef role;
    private PermissionRef permission;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public RolePermission(Long id, RoleRef role, PermissionRef permission,
            LocalDateTime createdDate, boolean enabled) {
        if (role == null)
            throw new IllegalArgumentException("role is required");
        if (permission == null)
            throw new IllegalArgumentException("permission is required");
        this.id = id;
        this.role = role;
        this.permission = permission;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static RolePermission create(RoleRef role, PermissionRef permission) {
        return new RolePermission(null, role, permission, LocalDateTime.now(), true);
    }

    public Long getId() {
        return id;
    }

    public RoleRef getRole() {
        return role;
    }

    public PermissionRef getPermission() {
        return permission;
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
