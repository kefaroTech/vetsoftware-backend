package com.vetsoftware.app.employee.application.command;

/**
 * Reenvío de invitación con nueva contraseña provisional. {@code companyId} lo
 * inyecta el controller desde el contexto autenticado (defensa en profundidad
 * del {@code @PreAuthorize}); el front nunca lo elige.
 */
public record ResendInvitationCommand(Long employeeId, String password, Long companyId) {
}
