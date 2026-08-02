package com.vetsoftware.app.passwordreset.application.port.out;

/**
 * Aplica la nueva contraseña al empleado (hashea, limpia mustChangePassword e invalida sesiones
 * vivas). Puerto hacia la feature employee; recibe la contraseña en claro y la feature employee la
 * hashea.
 */
public interface EmployeePasswordResetter {
  void reset(Long employeeId, String rawNewPassword);
}
