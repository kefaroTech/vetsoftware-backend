package com.vetsoftware.app.securityincident.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * <strong>Contencion y causa raiz son obligatorias</strong>, espejo de
 * {@code chk_security_incidents_close}. Un incidente que no se documento en su
 * momento es indistinguible de uno que se oculto, y esa es la unica diferencia
 * que un tercero puede comprobar despues.
 *
 * @param notifiedSubjectsAt
 *            cuando se informo a los titulares, si se hizo.
 *            <strong>Opcional</strong>: en Colombia la obligacion legal es
 *            informar a la autoridad, no a los titulares, y por eso esta fecha
 *            no tiene plazo asociado
 */
public record CloseSecurityIncidentRequest(
        @NotNull(message = "Debes indicar cuando se cerro el incidente.") LocalDateTime closedAt,
        @NotBlank(message = "Debes escribir como se contuvo el incidente.") String containment,
        @NotBlank(message = "Debes escribir la causa raiz del incidente.") String rootCause,
        @Schema(description = "Opcional: en Colombia la obligacion es informar a la autoridad, no a los titulares.") LocalDateTime notifiedSubjectsAt) {
}
