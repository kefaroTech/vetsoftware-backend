package com.vetsoftware.app.systemconfiguration.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigurationJpaRepository extends JpaRepository<SystemConfigurationJpaEntity, Long> {
    /** Busca por clave; el @SQLRestriction ya filtra enabled = true. */
    Optional<SystemConfigurationJpaEntity> findByPropertyName(String propertyName);
}
