package com.vetsoftware.app.employee.application.command;

/**
 * Cambio de la propia contraseña (primer login forzado). El {@code employeeId}
 * y el {@code companyId} los inyecta el controller desde el contexto
 * autenticado; el front nunca los elige. El {@code companyId} acota la lectura
 * del empleado: null es el caller sin empresa (SYSTEM).
 */
public record ChangeMyPasswordCommand(Long employeeId, String newPassword, Long companyId) {
}
