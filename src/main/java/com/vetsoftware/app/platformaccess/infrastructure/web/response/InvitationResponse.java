package com.vetsoftware.app.platformaccess.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lo unico que la pantalla de crear contrasena necesita saber. El correo sale
 * de la solicitud a la que apunta la invitacion, nunca de nada que mande el
 * cliente.
 */
public record InvitationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email) {
}
