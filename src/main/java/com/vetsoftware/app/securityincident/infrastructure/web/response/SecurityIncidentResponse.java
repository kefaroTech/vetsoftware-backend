package com.vetsoftware.app.securityincident.infrastructure.web.response;

import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.securityincident.domain.IncidentSeverity;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * El incidente tal como sale por HTTP. <strong>Solo lo ve la consola de
 * plataforma</strong>: los siete puertos de la rodaja estan cerrados a
 * {@code ROLE_SYSTEM} a secas.
 *
 * <p>
 * <strong>{@code deadlineAt} es el campo que hay que leer bien.</strong> Son
 * quince dias habiles contados desde {@code escalatedAt}, no desde
 * {@code detectedAt}: los dos instantes salen juntos justamente para que quien
 * mire la pantalla pueda ver la distancia entre enterarse y escalar, que es la
 * primera pregunta de cualquier auditoria. Un front que reste el plazo sobre
 * {@code detectedAt} pintaria un vencimiento mas tardio que el real.
 *
 * <p>
 * <strong>No lleva {@code version}</strong> —es una barandilla del que escribe,
 * no un dato del incidente— ni el conteo real de afectados:
 * {@code affectedSubjectCount} es el total declarado, y el reparto por clinica
 * se pide aparte.
 */
public record SecurityIncidentResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime detectedAt,
        @Schema(description = "Cuando ocurrio de verdad, si se supo. Nunca posterior a la deteccion.") LocalDateTime occurredAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "El escalamiento interno: desde aqui corren los 15 dias habiles del reporte a la SIC.") LocalDateTime escalatedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SecurityIncidentKind kind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) IncidentSeverity severity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String summary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Total declarado. El reparto real son las filas de afectados.") int affectedSubjectCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Vencimiento del reporte: 15 dias habiles desde el escalamiento, no desde la deteccion.") LocalDateTime deadlineAt,
        @Schema(description = "Nulo mientras no se haya reportado.") LocalDateTime reportedToAuthorityAt,
        @Schema(description = "El radicado. Va siempre junto a la fecha de reporte.") String reportReference,
        LocalDateTime notifiedSubjectsAt, String containment, String rootCause,
        @Schema(description = "Nulo mientras el incidente siga abierto.") LocalDateTime closedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static SecurityIncidentResponse from(SecurityIncidentDto dto) {
        return new SecurityIncidentResponse(dto.id(), dto.detectedAt(), dto.occurredAt(),
                dto.escalatedAt(), dto.kind(), dto.severity(), dto.summary(),
                dto.affectedSubjectCount(), dto.deadlineAt(), dto.reportedToAuthorityAt(),
                dto.reportReference(), dto.notifiedSubjectsAt(), dto.containment(), dto.rootCause(),
                dto.closedAt(), dto.createdDate());
    }
}
