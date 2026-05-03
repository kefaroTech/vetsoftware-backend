package com.vetsoftware.app.auth.application.port.out;

import java.util.Optional;

public interface EmployeeCredentialsRepository {
    Optional<EmployeeCredentials> findByCode(String employeeCode);

    record EmployeeCredentials(Long id, Long companyId, String hashPassword) {}
}
