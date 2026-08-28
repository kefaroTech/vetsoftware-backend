package com.vetsoftware.app.companylimitoverride.infrastructure.web.response;

import com.vetsoftware.app.companylimitoverride.application.dto.CompanyLimitOverrideDto;
import com.vetsoftware.app.companylimitoverride.domain.OverrideReasonCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una excepción negociada tal como la ven los frontends, <strong>con su firma
 * dentro</strong>.
 *
 * <p>
 * La respuesta lleva las dos mitades —la concesión y la revocación— porque el
 * listado es la <em>historia</em> de la empresa, revocadas incluidas: es lo que
 * responde «¿qué techo tenía el 14 de marzo?» sin reconstruir nada. Una
 * respuesta que solo enseñara las vivas convertiría esa pregunta en un trabajo
 * de arqueología.
 *
 * <p>
 * {@code alive} viaja calculado y no se deduce en el front: la vigencia depende
 * de dos campos a la vez ({@code revokedAt} y {@code validTo}) y dejar esa
 * conjunción a cada pantalla es garantizar que dos pantallas discrepen.
 */
public record CompanyLimitOverrideResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long limitDimensionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int limitQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate validFrom, LocalDate validTo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OverrideReasonCode reasonCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long grantedBySystemUserId,
        Long revokedBySystemUserId, LocalDateTime revokedAt, OverrideReasonCode revokedReasonCode,
        String revokedReason, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean alive) {

    public static CompanyLimitOverrideResponse from(CompanyLimitOverrideDto dto) {
        return new CompanyLimitOverrideResponse(dto.id(), dto.companyId(), dto.limitDimensionId(),
                dto.limitQuantity(), dto.validFrom(), dto.validTo(), dto.reasonCode(), dto.reason(),
                dto.grantedBySystemUserId(), dto.revokedBySystemUserId(), dto.revokedAt(),
                dto.revokedReasonCode(), dto.revokedReason(), dto.alive());
    }
}
