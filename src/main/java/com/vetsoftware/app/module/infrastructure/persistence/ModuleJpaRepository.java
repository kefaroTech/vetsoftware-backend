package com.vetsoftware.app.module.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleJpaRepository extends JpaRepository<ModuleJpaEntity, Long> {}
