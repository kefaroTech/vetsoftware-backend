package com.vetsoftware.app.subscription.application.command;

import java.time.LocalDate;

/**
 * Cambio de cantidad. Cierra la linea vigente y abre otra con la cantidad
 * nueva, arrastrando intactos el precio unitario y lo incluido: lo que se
 * renegocio fue cuantas unidades, no a que precio.
 *
 * <p>
 * <strong>No trae importes.</strong> El caso de uso los deriva de la diferencia
 * entre la sucesora y la original.
 */
public record ChangeSubscriptionItemQuantityCommand(Long id, Long companyId,
        Long subscriptionItemId, Integer newQuantity, String clientRequestId,
        LocalDate effectiveDate, String reason, Long requestedByEmployeeId,
        Long requestedBySystemUserId) {
}
