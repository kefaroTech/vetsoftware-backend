package com.vetsoftware.app.companyentitlementsnapshot.infrastructure.web.response;

import com.vetsoftware.app.companyentitlementsnapshot.application.dto.CompanyEntitlementSnapshotDto;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotTriggerReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * La foto de un recálculo de permisos, tal como la ven los frontends.
 *
 * <p>
 * <strong>{@code payload} viaja como cadena y no como objeto tipado</strong>, y
 * eso es el diseño y no una simplificación. Esta rodaja no conoce la forma de
 * los permisos —por eso el documento es JSON y no columnas—, y así la bitácora
 * no se rompe cada vez que esa tabla evoluciona. {@code payloadFormatVersion}
 * es lo que permite leer una foto vieja sabiendo con qué reglas se escribió: es
 * la diferencia entre una prueba y un blob que nadie sabe interpretar.
 *
 * <p>
 * <strong>El actor viaja desplegado en tres campos</strong> por la misma razón
 * que en la bitácora de cupo: exactamente uno está relleno y el motor lo
 * impone, así que un envoltorio con dos campos siempre vacíos no describiría
 * mejor la fila.
 */
public record CompanyEntitlementSnapshotResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime recalculatedAt,
        Long actorEmployeeId, Long actorSystemUserId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean actorIsProcess,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SnapshotTriggerReason triggerReason,
        Long amendmentId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String payload,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int payloadFormatVersion) {

    public static CompanyEntitlementSnapshotResponse from(CompanyEntitlementSnapshotDto dto) {
        return new CompanyEntitlementSnapshotResponse(dto.id(), dto.companyId(),
                dto.recalculatedAt(), dto.actorEmployeeId(), dto.actorSystemUserId(),
                dto.actorIsProcess(), dto.triggerReason(), dto.amendmentId(), dto.payload(),
                dto.payloadFormatVersion());
    }
}
