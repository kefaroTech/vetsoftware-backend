package com.vetsoftware.app.companylimitevent.infrastructure.web.response;

import com.vetsoftware.app.companylimitevent.application.dto.CompanyLimitEventDto;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Un hecho de la bitácora de cupo tal como lo ven los frontends.
 *
 * <p>
 * Que el cliente lea esto es lo que dice la ficha de construcción para el
 * bloque <em>Contadores y bitácora de cupo</em>: escribe plataforma,
 * <strong>leen ambos</strong>, y «el cliente ve sus propios portazos». Sin
 * ello, «¿cuántas veces topé el techo en marzo?» no tiene respuesta y la
 * oportunidad comercial que hay detrás se pierde entera.
 *
 * <p>
 * <strong>{@code limitSource} viaja como texto con su lista cerrada anotada, no
 * como tipo con nombre propio</strong>, y es a propósito. Hay dos enumerados
 * {@code LimitSource} en el árbol —el de esta rodaja, que copia el origen del
 * techo en el momento del hecho, y el de {@code companylimitoverride}, que lo
 * resuelve— y springdoc funde por nombre simple: exponer los dos como tipo
 * dejaría un único esquema {@code LimitSource} en el contrato que hoy cuadra
 * <em>por casualidad</em>, porque sus cuatro valores coinciden. El día que uno
 * de los dos gane un valor, los dos fronts leerían una lista que no es la suya.
 * Con {@code allowableValues} la lista viaja igual de cerrada y sin esquema
 * compartido.
 *
 * <p>
 * <strong>El actor viaja desplegado en tres campos y no como objeto</strong>,
 * porque exactamente uno está relleno y el motor lo impone: es más honesto
 * enseñar los tres huecos que inventar un envoltorio que solo tiene sentido con
 * dos de sus campos vacíos.
 */
public record CompanyLimitEventResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long limitDimensionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LimitEventType eventType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int limitQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int usedQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int requestedDelta,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "De dónde salía el techo en el momento del hecho", allowableValues = {
                "COMPANY_OVERRIDE", "SUBSCRIPTION", "CATALOG_DEFAULT", "NONE"}) String limitSource,
        Long overrideId, Long actorEmployeeId, Long actorSystemUserId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean actorIsProcess,
        String reasonCode, String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime occurredAt) {

    public static CompanyLimitEventResponse from(CompanyLimitEventDto dto) {
        return new CompanyLimitEventResponse(dto.id(), dto.companyId(), dto.limitDimensionId(),
                dto.eventType(), dto.limitQuantity(), dto.usedQuantity(), dto.requestedDelta(),
                dto.limitSource().name(), dto.overrideId(), dto.actorEmployeeId(),
                dto.actorSystemUserId(), dto.actorIsProcess(), dto.reasonCode(), dto.reason(),
                dto.occurredAt());
    }
}
