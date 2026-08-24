package com.vetsoftware.app.entitlement.application.dto;

import java.time.LocalDateTime;

/**
 * Resultado de un recalculo: que contrato lo sostiene, cuantas filas quedaron y
 * cuando. No devuelve las filas porque quien dispara el recalculo --el alta de
 * contrato, la baja de una linea, el paso a mora-- no las necesita; quien las
 * necesita las lee despues.
 *
 * @param entitlementCount
 *            filas totales que le quedan a la empresa, derivadas mas manuales
 * @param manualGrantCount
 *            cuantas de esas son concesiones a mano que el recalculo respeto
 *            sin tocar. Va aparte porque es lo unico que la capa derivada no
 *            puede reconstruir, y verlo en cero cuando deberia ser uno es la
 *            senal de que algo se las llevo
 */
public record EntitlementRecalculationDto(Long companyId, Long subscriptionId,
        String contractStatus, int entitlementCount, int manualGrantCount, int capacityCount,
        LocalDateTime recalculatedAt) {
}
