package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "base_role_permissions")
public class BaseRolePermissionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "base_role_id", nullable = false)
    private BaseRoleJpaEntity baseRole;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "base_permission_id", nullable = false)
    private BasePermissionJpaEntity basePermission;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    public BaseRolePermissionJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BaseRoleJpaEntity getBaseRole() { return baseRole; }
    public void setBaseRole(BaseRoleJpaEntity baseRole) { this.baseRole = baseRole; }
    public BasePermissionJpaEntity getBasePermission() { return basePermission; }
    public void setBasePermission(BasePermissionJpaEntity basePermission) { this.basePermission = basePermission; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
