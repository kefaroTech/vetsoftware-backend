package com.vetsoftware.app.companytrialgrant.application.port.out;

import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida de las concesiones.
 *
 * <p>
 * <strong>No declara borrado ni desactivación, y esa ausencia es la
 * regla</strong> (R-TRIAL-22): una prueba concedida no se puede desconceder. La
 * tabla tampoco lleva {@code enabled}, así que no hay ni siquiera la puerta de
 * atrás del borrado lógico.
 */
public interface CompanyTrialGrantRepository {

    CompanyTrialGrant save(CompanyTrialGrant grant);

    Optional<CompanyTrialGrant> findByCompanyIdAndCatalogItemId(Long companyId, Long catalogItemId);

    boolean existsByCompanyIdAndCatalogItemId(Long companyId, Long catalogItemId);

    List<CompanyTrialGrant> findAllByCompanyId(Long companyId);

    /**
     * El barrido de plataforma: las pruebas vivas que ya vencieron.
     *
     * <p>
     * No lleva empresa a propósito —es uno de los barridos globales— y por eso el
     * caso de uso que lo consume nace cerrado a un principal cross-tenant. Su
     * hermano acotado por empresa es {@link #findAllByCompanyId(Long)}.
     */
    List<CompanyTrialGrant> findLiveExpiredOn(LocalDate day);
}
