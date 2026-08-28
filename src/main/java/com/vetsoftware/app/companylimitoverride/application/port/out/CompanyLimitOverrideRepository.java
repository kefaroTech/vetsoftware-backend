package com.vetsoftware.app.companylimitoverride.application.port.out;

import com.vetsoftware.app.companylimitoverride.domain.CompanyLimitOverride;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida de las excepciones negociadas.
 *
 * <p>
 * Todas las lecturas llevan la empresa. No hay ninguna «por id» suelta: una
 * excepción es de alguien, y cargarla por un id que escribe el cliente sin
 * decir de quién es exactamente la familia de fugas que BE-COV cerró.
 */
public interface CompanyLimitOverrideRepository {

    CompanyLimitOverride save(CompanyLimitOverride override);

    Optional<CompanyLimitOverride> findAliveByCompanyIdAndLimitDimensionId(Long companyId,
            Long limitDimensionId);

    Optional<CompanyLimitOverride> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsAliveByCompanyIdAndLimitDimensionId(Long companyId, Long limitDimensionId);

    /** La historia completa de una empresa, revocadas incluidas. */
    List<CompanyLimitOverride> findAllByCompanyId(Long companyId);
}
