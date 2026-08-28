package com.vetsoftware.app.companylimitoverride.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consultas de las excepciones negociadas, todas acotadas por empresa.
 *
 * <p>
 * «Viva» son las dos condiciones —sin revocar y sin fecha de fin—, escritas
 * aquí igual que en la columna generada de la que cuelga el índice único. Si
 * las dos definiciones se separan, el código creerá que puede abrir una
 * excepción que el motor rechaza, o al revés.
 *
 * <p>
 * Sin consultas de escritura masiva: revocar pasa por el agregado, donde el
 * bloqueo optimista protege la fila.
 */
public interface CompanyLimitOverrideJpaRepository
        extends
            JpaRepository<CompanyLimitOverrideJpaEntity, Long> {

    Optional<CompanyLimitOverrideJpaEntity> findByCompanyIdAndLimitDimensionIdAndRevokedAtIsNullAndValidToIsNull(
            Long companyId, Long limitDimensionId);

    boolean existsByCompanyIdAndLimitDimensionIdAndRevokedAtIsNullAndValidToIsNull(Long companyId,
            Long limitDimensionId);

    Optional<CompanyLimitOverrideJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    List<CompanyLimitOverrideJpaEntity> findAllByCompanyIdOrderByLimitDimensionIdAscValidFromDescIdDesc(
            Long companyId);
}
