package com.vetsoftware.app.securityincident.infrastructure.web.request;

import com.vetsoftware.app.securityincident.domain.IncidentSeverity;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code companyId}</strong>, y aqui ni siquiera es la regla de
 * siempre: es que no existe la columna. {@code security_incidents} es una tabla
 * de plataforma y el reparto por clinica va por
 * {@code POST /system/security-incidents/{id}/affected-companies/{companyId}}.
 *
 * <p>
 * <strong>Sin {@code deadlineAt}</strong>, y esa ausencia es una decision de
 * seguridad: el vencimiento lo calcula el servidor contra el calendario
 * laboral. Aceptarlo por el cuerpo permitiria declarar un plazo mas largo que
 * el legal escribiendo un numero, que es el unico modo de incumplir sin que
 * nada avise.
 *
 * @param escalatedAt
 *            <strong>el escalamiento interno</strong>: cuando el incidente
 *            llego al area que lo atiende. Es el punto desde el que corren los
 *            quince dias habiles de la SIC, y por eso es obligatorio. Nunca
 *            anterior a la deteccion —esa mitad la valida el dominio, que es
 *            una regla entre dos campos y no de uno—
 * @param occurredAt
 *            cuando ocurrio de verdad. Nulo es legitimo: muchas veces solo se
 *            conoce la deteccion
 */
public record RegisterSecurityIncidentRequest(
        @NotNull(message = "Debes indicar cuando se detecto el incidente.") LocalDateTime detectedAt,
        LocalDateTime occurredAt,
        @NotNull(message = "Debes indicar cuando se escalo internamente el incidente.") @Schema(description = "El escalamiento interno. Desde aqui corren los 15 dias habiles del reporte a la SIC, no desde la deteccion.") LocalDateTime escalatedAt,
        @NotNull(message = "Debes indicar la clase de incidente.") SecurityIncidentKind kind,
        @NotNull(message = "Debes indicar la gravedad.") IncidentSeverity severity,
        @NotBlank(message = "El resumen es obligatorio.") @Size(max = 255, message = "El resumen no puede superar los 255 caracteres.") String summary,
        @PositiveOrZero(message = "El numero de titulares afectados no puede ser negativo.") int affectedSubjectCount) {
}
