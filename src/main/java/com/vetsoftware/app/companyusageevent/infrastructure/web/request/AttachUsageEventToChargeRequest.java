package com.vetsoftware.app.companyusageevent.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Colgar el hecho del cargo que lo facturo.
 *
 * <p>
 * Sin {@code companyId} en el cuerpo: viaja como {@code @RequestParam}, igual
 * que en el alta. El cargo tiene que ser de esa misma empresa, y de eso se
 * encarga tambien la base con {@code fk_cue_charge (company_id, charge_id)}.
 */
public record AttachUsageEventToChargeRequest(
        @NotNull(message = "Debes indicar el cargo.") @Positive(message = "El identificador del cargo debe ser positivo.") Long chargeId) {
}
