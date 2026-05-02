package com.vetsoftware.app.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_user_permissions")
public class SystemUserPermissionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_user_id", nullable = false)
    private Long systemUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_permission_id", nullable = false)
    private SystemPermissionJpaEntity systemPermission;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected SystemUserPermissionJpaEntity() {}

    public Long getId() { return id; }
    public Long getSystemUserId() { return systemUserId; }
    public SystemPermissionJpaEntity getSystemPermission() { return systemPermission; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
