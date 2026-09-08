package com.vetsoftware.app.subscription.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrialSubscriptionItemJpaRepository
        extends
            JpaRepository<SubscriptionItemJpaEntity, Long> {

    /** Vacío cuando una compra a mitad de prueba ya cerró la línea. */
    @EntityGraph(attributePaths = {"company", "subscription"})
    @Query("""
            SELECT i
            FROM SubscriptionItemJpaEntity i
            WHERE i.company.id = :companyId
              AND i.catalogItemId = :catalogItemId
              AND i.chargeMode = 'TRIAL'
              AND i.effectiveTo IS NULL
            """)
    Optional<SubscriptionItemJpaEntity> findOpenTrialLine(@Param("companyId") Long companyId,
            @Param("catalogItemId") Long catalogItemId);
}
