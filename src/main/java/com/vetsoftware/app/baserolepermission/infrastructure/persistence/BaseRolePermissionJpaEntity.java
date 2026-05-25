package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import java.time.LocalDateTime;

@Entity
@Table(name = "base_role_permissions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_base_role_permissions", columnNames = {"base_role_id", "base_permission_id"})
})
@SoftDelete(strategy = SoftDeleteType.ACTIVE, columnName = "enabled")
public class BaseRolePermissionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_role_id", nullable = false)
    private BaseRoleJpaEntity baseRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_permission_id", nullable = false)
    private BasePermissionJpaEntity basePermission;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public BaseRolePermissionJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BaseRoleJpaEntity getBaseRole() { return baseRole; }
    public void setBaseRole(BaseRoleJpaEntity baseRole) { this.baseRole = baseRole; }
    public BasePermissionJpaEntity getBasePermission() { return basePermission; }
    public void setBasePermission(BasePermissionJpaEntity basePermission) { this.basePermission = basePermission; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
