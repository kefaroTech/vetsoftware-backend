package com.vetsoftware.app.entitlement.infrastructure.web.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que la empresa del usuario puede usar y consumir ahora mismo: la respuesta
 * que consulta la aplicacion al arrancar para saber que menu pintar.
 *
 * <p>
 * {@code recalculatedAt} es el mas antiguo de la empresa. Va aqui porque si se
 * queda viejo hay un proceso caido, y sin exponerlo nadie fuera de la base de
 * datos podria notarlo.
 */
public record CompanyAccessResponse(Long companyId, List<CompanyEntitlementResponse> entitlements,
        List<CompanyCapacityResponse> capacities, LocalDateTime recalculatedAt) {
}
