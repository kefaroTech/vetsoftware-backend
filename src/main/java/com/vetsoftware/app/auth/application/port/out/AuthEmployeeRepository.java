package com.vetsoftware.app.auth.application.port.out;

import java.util.Optional;

public interface AuthEmployeeRepository {
    Optional<AuthEmployee> findActiveById(Long employeeId);

    /** Incrementa authVersion: invalida de inmediato todos los access tokens vivos del empleado. */
    void bumpAuthVersion(Long employeeId);

    record AuthEmployee(Long id, Long companyId, Long authVersion) {}
}
