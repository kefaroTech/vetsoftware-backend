package com.vetsoftware.app.systemuserpermission.domain;

import java.time.LocalDateTime;

public class SystemUserPermission {
    private Long id;
    private SystemUserRef systemUser;
    private SystemPermissionRef systemPermission;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public SystemUserPermission(Long id, SystemUserRef systemUser,
            SystemPermissionRef systemPermission, LocalDateTime createdDate, boolean enabled) {
        if (systemUser == null)
            throw new IllegalArgumentException("systemUser is required");
        if (systemPermission == null)
            throw new IllegalArgumentException("systemPermission is required");
        this.id = id;
        this.systemUser = systemUser;
        this.systemPermission = systemPermission;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static SystemUserPermission create(SystemUserRef systemUser,
            SystemPermissionRef systemPermission) {
        return new SystemUserPermission(null, systemUser, systemPermission, LocalDateTime.now(),
                true);
    }

    public void update(SystemUserRef systemUser, SystemPermissionRef systemPermission) {
        if (systemUser == null)
            throw new IllegalArgumentException("systemUser is required");
        if (systemPermission == null)
            throw new IllegalArgumentException("systemPermission is required");
        this.systemUser = systemUser;
        this.systemPermission = systemPermission;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public Long getId() {
        return id;
    }

    public SystemUserRef getSystemUser() {
        return systemUser;
    }

    public SystemPermissionRef getSystemPermission() {
        return systemPermission;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
