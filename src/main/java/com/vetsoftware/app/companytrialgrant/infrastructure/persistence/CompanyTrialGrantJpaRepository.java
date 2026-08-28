package com.vetsoftware.app.companytrialgrant.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Consultas de las concesiones.
 *
 * <p>
 * <strong>Ni una sola consulta de escritura masiva.</strong> No es un hueco: la
 * fecha de una concesión no se mueve nunca, y la única mutación —escribir el
 * desenlace— pasa por el agregado, donde el bloqueo optimista sí protege.
 */
public interface CompanyTrialGrantJpaRepository
        extends
            JpaRepository<CompanyTrialGrantJpaEntity, Long> {

    Optional<CompanyTrialGrantJpaEntity> findByCompanyIdAndCatalogItemId(Long companyId,
            Long catalogItemId);

    boolean existsByCompanyIdAndCatalogItemId(Long companyId, Long catalogItemId);

    List<CompanyTrialGrantJpaEntity> findAllByCompanyIdOrderByGrantedOnAscIdAsc(Long companyId);

    /**
     * El barrido de vencimientos, recorriendo
     * {@code ix_company_trial_grants_sweep}: igualdad primero
     * ({@code consumed_at IS NULL}) y rango después. El último día es inclusivo, de
     * ahí el {@code <=}.
     */
    @Query("""
            SELECT g FROM CompanyTrialGrantJpaEntity g
            WHERE g.consumedAt IS NULL AND g.trialEndDate <= :day
            ORDER BY g.trialEndDate ASC, g.id ASC
            """)
    List<CompanyTrialGrantJpaEntity> findLiveExpiredOn(@Param("day") LocalDate day);
}
