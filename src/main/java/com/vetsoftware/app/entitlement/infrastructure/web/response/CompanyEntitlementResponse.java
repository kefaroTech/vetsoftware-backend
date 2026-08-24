package com.vetsoftware.app.entitlement.infrastructure.web.response;

import java.time.LocalDateTime;

/**
 * Un permiso derivado tal como lo ven los dos frontends.
 *
 * <p>
 * {@code accessLevel} es {@code FULL}, {@code READ_ONLY} o {@code NONE}, y es
 * lo que decide si la interfaz pinta el modulo, lo pinta sin botones de
 * guardar, o no lo pinta.
 */
public record CompanyEntitlementResponse(Long id, Long companyId, SubModuleSummary subModule,
        String accessLevel, String source, Long subscriptionId, Long subscriptionItemId,
        LocalDateTime validFrom, LocalDateTime validUntil, LocalDateTime recalculatedAt) {
}
