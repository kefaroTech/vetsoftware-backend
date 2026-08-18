package com.vetsoftware.app.auth.application.port.out;

import java.util.Optional;

public interface AuthEmployeeRepository {
    Optional<AuthEmployee> findActiveById(Long employeeId);

    /**
     * Incrementa la versión bajo bloqueo de fila y devuelve el estado autenticable
     * actualizado. Serializa logins concurrentes para que solo la última versión
     * emitida permanezca válida.
     */
    Optional<AuthEmployee> rotateAuthVersion(Long employeeId);

    /**
     * Incrementa authVersion: invalida de inmediato todos los access tokens vivos
     * del empleado. Sin acotar — es el camino del refresh, donde el sujeto sale del
     * token ya validado y no hay empresa en el contexto.
     */
    void bumpAuthVersion(Long employeeId);

    /**
     * Igual que {@link #bumpAuthVersion(Long)} pero acotado a la empresa, para el
     * logout: ahi el {@code companyId} viene del principal y el gate del puerto
     * ({@code isAuthenticated()}) no dice nada sobre de quien es la fila.
     */
    void bumpAuthVersion(Long employeeId, Long companyId);

    record AuthEmployee(Long id, Long companyId, Long authVersion) {
    }
}
