package com.vetsoftware.app.companyentitlementsnapshot.application.port.in;

import com.vetsoftware.app.companyentitlementsnapshot.application.dto.CompanyEntitlementSnapshotDto;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Qué veía una empresa un día concreto.
 *
 * <p>
 * Es la pregunta entera por la que existe la tabla: un salto y una fila. Sin
 * ella, «demuéstrame qué permisos tenía el 3 de marzo» no tiene respuesta,
 * porque la tabla de permisos se reescribe en cada recálculo.
 */
public interface FindEntitlementSnapshotAsOfUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyEntitlementSnapshot.read')"
            + " and @authz.isMyCompany(#companyId))")
    CompanyEntitlementSnapshotDto findLatestAsOf(Long companyId, LocalDateTime at);
}
