package com.vetsoftware.app.systemuserpermission.domain;

import java.time.LocalDateTime;

public class SystemUserPermission {
    private Long id;
    private SystemUserRef systemUser;
    private SystemPermissionRef systemPermission;
    private final LocalDateTime createdDate;

    public SystemUserPermission(Long id, SystemUserRef systemUser, SystemPermissionRef systemPermission,
                                LocalDateTime createdDate) {
        if (systemUser == null) throw new IllegalArgumentException("systemUser is required");
        if (systemPermission == null) throw new IllegalArgumentException("systemPermission is required");
        this.id = id;
        this.systemUser = systemUser;
        this.systemPermission = systemPermission;
        this.createdDate = createdDate;
    }

    public static SystemUserPermission create(SystemUserRef systemUser, SystemPermissionRef systemPermission) {
        return new SystemUserPermission(null, systemUser, systemPermission, LocalDateTime.now());
    }

    public void update(SystemUserRef systemUser, SystemPermissionRef systemPermission) {
        if (systemUser == null) throw new IllegalArgumentException("systemUser is required");
        if (systemPermission == null) throw new IllegalArgumentException("systemPermission is required");
        this.systemUser = systemUser;
        this.systemPermission = systemPermission;
    }

    public Long getId() { return id; }
    public SystemUserRef getSystemUser() { return systemUser; }
    public SystemPermissionRef getSystemPermission() { return systemPermission; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
