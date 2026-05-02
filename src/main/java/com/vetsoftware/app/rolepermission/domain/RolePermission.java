package com.vetsoftware.app.rolepermission.domain;

import java.time.LocalDateTime;

public class RolePermission {
    private Long id;
    private RoleRef role;
    private PermissionRef permission;
    private final LocalDateTime createdDate;

    public RolePermission(Long id, RoleRef role, PermissionRef permission, LocalDateTime createdDate) {
        if (role == null) throw new IllegalArgumentException("role is required");
        if (permission == null) throw new IllegalArgumentException("permission is required");
        this.id = id;
        this.role = role;
        this.permission = permission;
        this.createdDate = createdDate;
    }

    public static RolePermission create(RoleRef role, PermissionRef permission) {
        return new RolePermission(null, role, permission, LocalDateTime.now());
    }

    public void update(RoleRef role, PermissionRef permission) {
        if (role == null) throw new IllegalArgumentException("role is required");
        if (permission == null) throw new IllegalArgumentException("permission is required");
        this.role = role;
        this.permission = permission;
    }

    public Long getId() { return id; }
    public RoleRef getRole() { return role; }
    public PermissionRef getPermission() { return permission; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
