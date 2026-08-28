package com.vetsoftware.app.companylimitoverride.infrastructure.web.request;

import com.vetsoftware.app.companylimitoverride.domain.OverrideReasonCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cerrar una excepción negociada.
 *
 * <p>
 * <strong>Ni la empresa ni el eje ni quién revoca viajan aquí</strong>: los dos
 * primeros identifican la fila y van en la ruta; el tercero lo pone el servidor
 * con {@code authz.currentSystemUserId()}.
 *
 * <p>
 * El motivo de la revocación es tan obligatorio como el de la concesión, y por
 * lo mismo: una excepción que se quita sin explicación deja el informe con un
 * hueco justo donde estaba la decisión.
 */
public record RevokeCompanyLimitOverrideRequest(
        @NotNull(message = "Debes indicar el tipo de motivo de la revocación.") OverrideReasonCode revokedReasonCode,
        @NotBlank(message = "El motivo de la revocación es obligatorio.") @Size(max = 255, message = "El motivo de la revocación no puede superar los 255 caracteres.") String revokedReason) {
}
