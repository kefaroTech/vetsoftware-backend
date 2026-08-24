package com.vetsoftware.app.dunning.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Sin {@code @Query} de {@code UPDATE} ni de {@code DELETE}: la tabla es
 * append-only y no hay nada que mutar.
 */
public interface DunningEventJpaRepository extends JpaRepository<DunningEventJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"subscription", "billingDocument"})
    Page<DunningEventJpaEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"subscription", "billingDocument"})
    Optional<DunningEventJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Acotado por contrato <strong>y</strong> por empresa. Filtrar solo por
     * {@code subscriptionId} no cuenta como aislamiento: el contrato es de alguien
     * (mismo criterio que BE-29).
     */
    @EntityGraph(attributePaths = {"subscription", "billingDocument"})
    Page<DunningEventJpaEntity> findAllBySubscription_IdAndCompanyId(Long subscriptionId,
            Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"subscription", "billingDocument"})
    Page<DunningEventJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);
}
