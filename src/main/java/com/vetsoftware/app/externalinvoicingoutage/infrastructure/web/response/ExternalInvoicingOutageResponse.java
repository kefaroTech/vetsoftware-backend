package com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.response;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.domain.CauseParty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * La caida tal como sale por HTTP. <strong>La ve solo la consola de
 * plataforma</strong>: los ocho endpoints de esta rodaja estan cerrados a
 * {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * <strong>No lleva {@code version}</strong> —es una barandilla del que escribe,
 * no un dato de la caida— ni la columna generada {@code open_outage_marker}: es
 * detalle del motor, existe para que {@code uq_eio_open} pueda restringir lo
 * que con {@code NULL} no restringia, y publicarla invitaria a construir logica
 * sobre un centinela de base de datos. Lo que si sale es {@code open}, que es
 * la misma pregunta contestada desde el modelo y no desde el esquema.
 *
 * <p>
 * <strong>{@code affectedCompanyCount} es un contador de conveniencia.</strong>
 * La verdad esta en la puente, y este numero puede ir por detras mientras se
 * arma el reparto. Se publica porque en caliente es lo unico que hay.
 */
public record ExternalInvoicingOutageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startedAt,
        @Schema(description = "Nulo mientras la caida sigue viva.") LocalDateTime endedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Separa un incidente de un incumplimiento propio.") CauseParty causeParty,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String summary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Contador de conveniencia: la verdad esta en el reparto por clinica.") int affectedCompanyCount,
        @Schema(description = "Nulo mientras no se haya avisado.") LocalDateTime notifiedCompaniesAt,
        @Schema(description = "El radicado del proveedor externo.") String externalIncidentRef,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Si la caida sigue viva.") boolean open,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static ExternalInvoicingOutageResponse from(ExternalInvoicingOutageDto dto) {
        return new ExternalInvoicingOutageResponse(dto.id(), dto.startedAt(), dto.endedAt(),
                dto.causeParty(), dto.summary(), dto.affectedCompanyCount(),
                dto.notifiedCompaniesAt(), dto.externalIncidentRef(), dto.open(),
                dto.createdDate());
    }
}
