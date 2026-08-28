package com.vetsoftware.app.accountingperiod.infrastructure.web.response;

import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * El mes contable tal como sale por HTTP. <strong>Solo la ve la consola de
 * plataforma</strong>: no hay camino de tenant en esta feature.
 *
 * <p>
 * Los cinco campos de cierre y reapertura van <strong>sin</strong>
 * {@code REQUIRED} a proposito: son nulos en un mes abierto que nunca se cerro,
 * que es el estado normal del mes en curso. Marcarlos obligatorios haria que el
 * tipo generado para el front prometiera valores que la mayoria de las filas no
 * tiene, y el front acabaria pintando «null» donde no hay nada que contar.
 *
 * <p>
 * {@code periodKey} sale como cadena plana —{@code "2026-03"}— y no como el
 * value object del dominio: springdoc generaria si no un objeto anidado
 * {@code {"value": "2026-03"}} y el front tendria que desenvolverlo en cada
 * pantalla.
 */
public record AccountingPeriodResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-03") String periodKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AccountingPeriodStatus status,
        LocalDateTime closedAt, Long closedBySystemUserId, LocalDateTime reopenedAt,
        Long reopenedBySystemUserId, String reopenedReason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static AccountingPeriodResponse from(AccountingPeriodDto dto) {
        return new AccountingPeriodResponse(dto.id(), dto.periodKey(), dto.status(), dto.closedAt(),
                dto.closedBySystemUserId(), dto.reopenedAt(), dto.reopenedBySystemUserId(),
                dto.reopenedReason(), dto.createdDate());
    }
}
