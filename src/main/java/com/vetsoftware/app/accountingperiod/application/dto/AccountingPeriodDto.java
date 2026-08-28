package com.vetsoftware.app.accountingperiod.application.dto;

import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import java.time.LocalDateTime;

/**
 * El mes contable tal como lo consume la aplicacion.
 *
 * <p>
 * <strong>La clave viaja como {@code String} y no como
 * {@code AccountingPeriodKey}.</strong> El value object es una invariante del
 * dominio, no una forma de transporte: publicarlo aqui haria que springdoc
 * generara para los dos fronts un objeto anidado
 * {@code {"periodKey": {"value": "2026-03"}}} en vez de la cadena que la ficha
 * es. El sitio donde se valida el formato sigue siendo el constructor del VO.
 *
 * <p>
 * <strong>Sin {@code version}</strong>: el numero de version es la barandilla
 * del bloqueo optimista, no un dato del expediente. Publicarlo invitaria a un
 * cliente a mandarlo de vuelta y a construir un control de concurrencia
 * paralelo al que ya hace Hibernate.
 *
 * <p>
 * <strong>Los dos ids de firma si salen</strong>, y en crudo: la consola de
 * plataforma resuelve el nombre por su cuenta. Copiar aqui el nombre de quien
 * cerro lo congelaria mal el dia que esa persona cambie de nombre, y lo que la
 * ficha guarda es quien firmo, no como se llamaba entonces.
 */
public record AccountingPeriodDto(Long id, String periodKey, AccountingPeriodStatus status,
        LocalDateTime closedAt, Long closedBySystemUserId, LocalDateTime reopenedAt,
        Long reopenedBySystemUserId, String reopenedReason, LocalDateTime createdDate) {

    public static AccountingPeriodDto from(AccountingPeriod period) {
        return new AccountingPeriodDto(period.getId(), period.getPeriodKey().value(),
                period.getStatus(), period.getClosedAt(), period.getClosedBySystemUserId(),
                period.getReopenedAt(), period.getReopenedBySystemUserId(),
                period.getReopenedReason(), period.getCreatedDate());
    }
}
