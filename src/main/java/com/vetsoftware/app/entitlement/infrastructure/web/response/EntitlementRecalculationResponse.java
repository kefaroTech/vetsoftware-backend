package com.vetsoftware.app.entitlement.infrastructure.web.response;

import java.time.LocalDateTime;

/**
 * Resultado de un recalculo: que contrato lo sostiene y cuantas filas quedaron.
 *
 * <p>
 * {@code manualGrantCount} se expone aparte porque es lo unico que la capa
 * derivada no reconstruye: el recalculo respeta esas filas sin tocarlas, y
 * verlo en cero cuando deberia ser uno es la senal de que algo se las llevo.
 */
public record EntitlementRecalculationResponse(Long companyId, Long subscriptionId,
        String contractStatus, int entitlementCount, int manualGrantCount, int capacityCount,
        LocalDateTime recalculatedAt) {
}
