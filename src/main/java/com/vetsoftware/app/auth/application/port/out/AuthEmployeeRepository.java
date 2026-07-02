package com.vetsoftware.app.auth.application.port.out;

import java.util.Optional;

public interface AuthEmployeeRepository {
    Optional<AuthEmployee> findActiveById(Long employeeId);

    record AuthEmployee(Long id, Long companyId, Long authVersion) {}
}
