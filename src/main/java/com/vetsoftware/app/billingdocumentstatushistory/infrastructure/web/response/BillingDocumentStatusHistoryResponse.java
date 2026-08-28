package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.web.response;

import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Un fotograma tal como sale por HTTP.
 *
 * <p>
 * <strong>Todos los campos son obligatorios, y que no haya ni un nulo es la
 * caracteristica.</strong> Es una bitacora que solo se agrega: no existe el
 * estado «este dato aun no se sabe». Si algun dia apareciera un campo opcional
 * aqui, la pregunta antes de añadirlo es si sigue siendo la misma tabla.
 *
 * <p>
 * <strong>{@code fromStatus} viaja aunque parezca redundante.</strong> Se
 * podria deducir mirando el fotograma anterior — pero solo si el consumidor
 * tiene la pelicula entera y en orden, que no es el caso cuando lee una pagina
 * de la bandeja por estado. Publicando el par, cada fila se explica sola.
 */
public record BillingDocumentStatusHistoryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long billingDocumentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BillingDocumentStatus fromStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BillingDocumentStatus toStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime occurredAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String actor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static BillingDocumentStatusHistoryResponse from(BillingDocumentStatusHistoryDto dto) {
        return new BillingDocumentStatusHistoryResponse(dto.id(), dto.companyId(),
                dto.billingDocumentId(), dto.fromStatus(), dto.toStatus(), dto.occurredAt(),
                dto.actor(), dto.reason(), dto.createdDate());
    }
}
