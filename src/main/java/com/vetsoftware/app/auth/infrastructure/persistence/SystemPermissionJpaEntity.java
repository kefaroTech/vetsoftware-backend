package com.vetsoftware.app.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_permissions")
public class SystemPermissionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected SystemPermissionJpaEntity() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
