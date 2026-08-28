package com.vetsoftware.app.companycontactchannel.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <strong>El motivo es obligatorio, y esa es la unica razon por la que este
 * cuerpo existe.</strong> Revocar podria haber sido un {@code PATCH} sin cuerpo
 * —la fecha la pone el servidor—, pero el esquema exige {@code revoked_reason}
 * junto a {@code revoked_at}: una revocacion sin motivo obliga a quien audite
 * el ano siguiente a adivinar si el cliente se dio de baja o si fue un error de
 * captura.
 *
 * <p>
 * <strong>Sin {@code companyId} y sin {@code revokedAt}</strong>: los pone el
 * servidor, por los mismos motivos que en el alta.
 */
public record RevokeCompanyContactChannelRequest(
        @NotBlank(message = "Debes indicar por que se revoca el canal.") @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.") String reason) {
}
