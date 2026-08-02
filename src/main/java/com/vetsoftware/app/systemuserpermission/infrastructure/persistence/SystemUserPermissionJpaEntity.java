package com.vetsoftware.app.systemuserpermission.infrastructure.persistence;

import com.vetsoftware.app.systempermission.infrastructure.persistence.SystemPermissionJpaEntity;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "system_user_permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_system_user_permissions", columnNames = {"system_user_id",
                "system_permission_id"})})
@SQLDelete(sql = "UPDATE system_user_permissions SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
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

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public SystemUserPermissionJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SystemUserJpaEntity getSystemUser() {
        return systemUser;
    }

    public void setSystemUser(SystemUserJpaEntity systemUser) {
        this.systemUser = systemUser;
    }

    public SystemPermissionJpaEntity getSystemPermission() {
        return systemPermission;
    }

    public void setSystemPermission(SystemPermissionJpaEntity systemPermission) {
        this.systemPermission = systemPermission;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
