package com.vetsoftware.app.entitlement.infrastructure.web.response;

import java.time.LocalDateTime;

/**
 * Un contador contratado. {@code exhausted} viene calculado para que la
 * interfaz pueda avisar antes de bloquear y ofrecer la ampliacion en el momento
 * exacto en que hace falta.
 */
public record CompanyCapacityResponse(Long id, Long companyId, String capacityUnit,
        int limitQuantity, int usedQuantity, boolean exhausted, Long subscriptionId,
        LocalDateTime recalculatedAt) {
}
