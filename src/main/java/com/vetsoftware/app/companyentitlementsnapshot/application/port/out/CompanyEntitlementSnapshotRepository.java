package com.vetsoftware.app.companyentitlementsnapshot.application.port.out;

import com.vetsoftware.app.companyentitlementsnapshot.domain.CompanyEntitlementSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida de las fotos de permisos.
 *
 * <p>
 * <strong>Solo se agrega.</strong> Ni actualización ni borrado: una foto que se
 * puede retocar no demuestra nada.
 */
public interface CompanyEntitlementSnapshotRepository {

    CompanyEntitlementSnapshot append(CompanyEntitlementSnapshot snapshot);

    /**
     * Qué veía la empresa un día concreto: la última foto anterior o igual a ese
     * momento.
     */
    Optional<CompanyEntitlementSnapshot> findLatestAsOf(Long companyId, LocalDateTime at);

    List<CompanyEntitlementSnapshot> findAllByCompanyIdBetween(Long companyId, LocalDateTime from,
            LocalDateTime to);
}
