package com.vetsoftware.app.platformaccess.application.dto;

/**
 * Lo único que la pantalla de aceptar invitación necesita saber: para qué
 * correo es. Sale de la solicitud a la que apunta la invitación, nunca del
 * cliente.
 */
public record PlatformInvitationDto(String email) {
}
