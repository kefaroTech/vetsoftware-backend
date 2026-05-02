package com.vetsoftware.app.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_users")
public class SystemUserJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "hash_password", nullable = false, length = 255)
    private String hashPassword;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected SystemUserJpaEntity() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getHashPassword() { return hashPassword; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
