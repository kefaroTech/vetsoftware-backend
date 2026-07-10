package com.vetsoftware.app.employee.application.command;

/** Cambio de la propia contraseña (primer login forzado). El {@code employeeId} lo inyecta el controller
 *  desde el contexto autenticado; el front nunca lo elige. */
public record ChangeMyPasswordCommand(Long employeeId, String newPassword) {}
