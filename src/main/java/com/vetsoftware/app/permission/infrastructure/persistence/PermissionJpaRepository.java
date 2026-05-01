package com.vetsoftware.app.permission.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionJpaRepository extends JpaRepository<PermissionJpaEntity, Long> {}
