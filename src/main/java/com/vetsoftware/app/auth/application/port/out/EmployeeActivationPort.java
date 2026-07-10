package com.vetsoftware.app.auth.application.port.out;

/**
 * Marca al empleado como ACTIVO en su primer login (INVITED → ACTIVE). Idempotente: si ya está activo no
 * hace nada. Lo consume {@code LoginEmployeeService} tras autenticar con éxito.
 */
public interface EmployeeActivationPort {
    void activateOnLogin(Long employeeId);
}
