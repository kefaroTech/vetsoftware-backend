package com.vetsoftware.app.platformaccess.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de aceptar la invitacion.
 *
 * <p>
 * <b>No lleva correo, y no debe llevarlo nunca.</b> El correo de la cuenta que
 * nace sale del token; uno que viajase aqui permitiria a quien posee una
 * invitacion legitima elegir la identidad del superadministrador que se va a
 * crear. Si algun dia alguien lo anade, el caso de uso lo ignora.
 *
 * <p>
 * El minimo son 12 caracteres y no los 8 del alta ordinaria de cuentas de
 * sistema: esta tiene control total sobre todos los tenants.
 */
public record AcceptInvitationRequest(
        @NotBlank(message = "El token es obligatorio.") @Size(max = 200, message = "El token no tiene un formato valido.") String token,
        @NotBlank(message = "La contrasena es obligatoria.") @Size(min = 12, max = 100, message = "La contrasena debe tener entre 12 y 100 caracteres.") String password) {
}
