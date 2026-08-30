package com.vetsoftware.app.registration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * El puente propuesta &rarr; empresa.
 *
 * <p>
 * <strong>No declara ni un solo {@code find...} que devuelva varias
 * filas</strong>, asi que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} no tiene
 * nada que gatear aqui. Los dos {@code exists} existen para que un reintento
 * del alta no choque contra los unicos de la tabla, no para listar nada.
 */
public interface AiProposalConversionJpaRepository
        extends
            JpaRepository<AiProposalConversionJpaEntity, Long> {

    /**
     * ¿Esta propuesta ya convirtio? Espejo de
     * {@code uq_ai_proposal_conversions_proposal}.
     */
    boolean existsByProposalId(Long proposalId);

    /**
     * ¿Esta empresa ya tiene propuesta atribuida? Espejo de
     * {@code uq_ai_proposal_conversions_company}.
     */
    boolean existsByCompanyId(Long companyId);
}
