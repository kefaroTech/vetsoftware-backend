package com.vetsoftware.app.companyusageevent.application.dto;

import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.companyusageevent.domain.UsageBranch;
import java.time.LocalDateTime;

/**
 * El hecho de uso tal como sale del caso de uso.
 *
 * <p>
 * <strong>Sin {@code version}</strong>: el numero de bloqueo optimista es una
 * barandilla del que escribe, no un dato del hecho. Publicarlo invitaria a que
 * un cliente lo devolviera y a construir un protocolo de concurrencia sobre una
 * bitacora que solo escribe el medidor.
 *
 * <p>
 * <strong>Sin {@code usageRefKey}</strong>: es una columna generada, detalle
 * del motor, y existe unicamente para que un indice unico pueda restringir lo
 * que con cuatro columnas nulables no restringia. Publicarla invitaria a
 * construir logica sobre un centinela de base de datos.
 */
public record CompanyUsageEventDto(Long id, Long companyId, Long limitDimensionId,
        UsageBranch branch, Long usageReferenceId, LocalDateTime occurredAt, String periodKey,
        boolean billable, Long chargeId, LocalDateTime createdDate) {

    public static CompanyUsageEventDto from(CompanyUsageEvent event) {
        return new CompanyUsageEventDto(event.getId(), event.getCompanyId(),
                event.getLimitDimensionId(), event.getBranch(), event.getUsageReferenceId(),
                event.getOccurredAt(), event.getPeriodKey().value(), event.isBillable(),
                event.getChargeId(), event.getCreatedDate());
    }
}
