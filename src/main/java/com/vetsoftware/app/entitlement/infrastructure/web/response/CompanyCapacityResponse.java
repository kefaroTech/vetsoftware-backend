package com.vetsoftware.app.entitlement.infrastructure.web.response;

import java.time.LocalDateTime;

/**
 * Un contador contratado. {@code exhausted} viene calculado para que la
 * interfaz pueda avisar antes de bloquear y ofrecer la ampliacion en el momento
 * exacto en que hace falta.
 *
 * <p>
 * El eje se identifica con {@code limitDimensionId} y {@code dimensionCode} en
 * lugar del antiguo {@code capacityUnit}, que era uno de cuatro valores fijos.
 * {@code measureKind} y {@code periodKey} viajan porque la interfaz los
 * necesita para nombrar bien el contador: un eje acumulativo se llama
 * "registradas historicamente" y no "activas" (R-LIMIT-40), y uno de flujo
 * tiene que decir de que periodo habla.
 *
 * <p>
 * {@code usageReconciledAt} llega {@code null} mientras el consumo no se haya
 * comprobado nunca contra las filas reales. No es lo mismo que
 * {@code limitRecalculatedAt} y por eso son dos campos (R-ENT-13).
 */
public record CompanyCapacityResponse(Long id, Long companyId, Long limitDimensionId,
        String dimensionCode, String measureKind, String periodKey, int limitQuantity,
        int usedQuantity, boolean exhausted, Long subscriptionId, LocalDateTime limitRecalculatedAt,
        LocalDateTime usageReconciledAt) {
}
