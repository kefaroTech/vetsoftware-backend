package com.vetsoftware.app.systemuserpermission.infrastructure.persistence;

import com.vetsoftware.app.systempermission.infrastructure.persistence.SystemPermissionJpaEntity;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_user_permissions")
public class SystemUserPermissionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_user_id", nullable = false)
    private SystemUserJpaEntity systemUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_permission_id", nullable = false)
    private SystemPermissionJpaEntity systemPermission;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    public SystemUserPermissionJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SystemUserJpaEntity getSystemUser() { return systemUser; }
    public void setSystemUser(SystemUserJpaEntity systemUser) { this.systemUser = systemUser; }
    public SystemPermissionJpaEntity getSystemPermission() { return systemPermission; }
    public void setSystemPermission(SystemPermissionJpaEntity systemPermission) { this.systemPermission = systemPermission; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
