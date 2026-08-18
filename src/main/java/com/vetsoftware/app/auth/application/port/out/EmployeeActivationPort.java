package com.vetsoftware.app.auth.application.port.out;

/**
 * Marca al empleado como ACTIVO en su primer login (INVITED → ACTIVE).
 * Idempotente: si ya está activo no hace nada. Lo consume
 * {@code LoginEmployeeService} tras autenticar con éxito.
 */
public interface EmployeeActivationPort {
    /**
     * @param companyId
     *            la empresa que trae la credencial ya verificada, no la de la fila
     *            que se va a actualizar: acota el UPDATE a la empresa que devolvio
     *            la lectura de credenciales.
     */
    void activateOnLogin(Long employeeId, Long companyId);
}
