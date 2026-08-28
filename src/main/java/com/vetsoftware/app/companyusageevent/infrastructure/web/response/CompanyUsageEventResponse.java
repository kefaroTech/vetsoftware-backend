package com.vetsoftware.app.companyusageevent.infrastructure.web.response;

import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.companyusageevent.domain.UsageBranch;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * El hecho de uso tal como sale por HTTP. Solo lo ve la consola de plataforma.
 *
 * <p>
 * <strong>{@code occurredAt} y {@code createdDate} son dos instantes distintos
 * y el front no puede confundirlos.</strong> El primero es cuando ocurrio el
 * hecho —la cita se agendo a las 09:14— y el segundo cuando se anoto —el
 * proceso nocturno, a las 03:00 del dia siguiente—. La columna que se ensena en
 * el expediente de una reclamacion es la primera; pintar la segunda haria que
 * todos los hechos de un lote parecieran simultaneos.
 *
 * <p>
 * <strong>No lleva {@code version}</strong> —es una barandilla del que escribe,
 * no un dato del hecho— ni {@code usageRefKey}, que es una columna generada:
 * existe para que un indice unico pueda restringir lo que con cuatro columnas
 * nulables no restringia, y publicarla invitaria a construir logica sobre un
 * centinela de base de datos.
 *
 * <p>
 * <strong>Publica la rama y una sola referencia</strong>, no las cuatro
 * columnas nulables del esquema: tres de ellas serian siempre nulas y la cuarta
 * obligaria al front a averiguar cual mirar. La forma que se expone es la del
 * dominio, que es la que no admite la combinacion invalida.
 */
public record CompanyUsageEventResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long limitDimensionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "El eje contable del hecho, que dice a que tabla apunta la referencia.") UsageBranch branch,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "El registro consumido, en la tabla que indica la rama.") Long usageReferenceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Cuando ocurrio el hecho. NO es cuando se anoto.") LocalDateTime occurredAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String periodKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean billable,
        @Schema(description = "El cargo que lo facturo. Nulo mientras el hecho no se haya cobrado.") Long chargeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Cuando se anoto el hecho.") LocalDateTime createdDate) {

    public static CompanyUsageEventResponse from(CompanyUsageEventDto dto) {
        return new CompanyUsageEventResponse(dto.id(), dto.companyId(), dto.limitDimensionId(),
                dto.branch(), dto.usageReferenceId(), dto.occurredAt(), dto.periodKey(),
                dto.billable(), dto.chargeId(), dto.createdDate());
    }
}
