package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_permissions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_role_permissions", columnNames = {"role_id", "permission_id"})
})
@SQLDelete(sql = "UPDATE role_permissions SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class RolePermissionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleJpaEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionJpaEntity permission;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public RolePermissionJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RoleJpaEntity getRole() { return role; }
    public void setRole(RoleJpaEntity role) { this.role = role; }
    public PermissionJpaEntity getPermission() { return permission; }
    public void setPermission(PermissionJpaEntity permission) { this.permission = permission; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
