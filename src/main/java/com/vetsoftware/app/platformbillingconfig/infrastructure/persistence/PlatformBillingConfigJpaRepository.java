package com.vetsoftware.app.platformbillingconfig.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformBillingConfigJpaRepository
        extends
            JpaRepository<PlatformBillingConfigJpaEntity, Long> {

    /**
     * La fila única, buscada por el discriminador que la hace única. Resuelve por
     * {@code uq_platform_billing_config_singleton}, así que devuelve como mucho una
     * fila por construcción del esquema, no por convenio.
     *
     * <p>
     * Se busca por {@code singleton} y no por {@code id = 1} porque {@code id} es
     * {@code AUTO_INCREMENT}: nada garantiza que la fila sembrada tenga el 1 si
     * alguna vez se recrea.
     */
    Optional<PlatformBillingConfigJpaEntity> findBySingleton(byte singleton);
}
