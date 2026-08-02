package com.vetsoftware.app.employee.application.command;

/**
 * Restablecimiento de la contraseña de un empleado (flujo "olvidé mi contraseña"). newPassword en
 * claro.
 */
public record ResetEmployeePasswordCommand(Long employeeId, String newPassword) {}
