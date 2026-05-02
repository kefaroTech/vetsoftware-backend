package com.vetsoftware.app.auth.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemUserJpaRepository extends JpaRepository<SystemUserJpaEntity, Long> {

    Optional<SystemUserJpaEntity> findByCode(String code);
}
