package com.vetsoftware.app.securityincident.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Los dos van juntos o no va ninguno: espejo de
 * {@code chk_security_incidents_report}. Un reporte sin radicado no se puede
 * rastrear, y uno que no se puede rastrear no consta.
 *
 * @param reportedAt
 *            la fecha del reporte <b>en el micrositio de la Delegatura</b>, no
 *            la de este registro. Viene por el cuerpo justamente por eso:
 *            sellarla con el reloj del servidor convertiria «se reporto el dia
 *            12» en «se registro el dia 19»
 */
public record ReportSecurityIncidentRequest(
        @NotNull(message = "Debes indicar cuando se reporto a la autoridad.") LocalDateTime reportedAt,
        @NotBlank(message = "El radicado del reporte es obligatorio.") @Size(max = 100, message = "El radicado no puede superar los 100 caracteres.") @Schema(description = "El radicado que devuelve el micrositio de la Delegatura de Proteccion de Datos.") String reportReference) {
}
