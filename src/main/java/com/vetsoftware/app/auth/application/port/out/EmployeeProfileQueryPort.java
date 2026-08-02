package com.vetsoftware.app.auth.application.port.out;

import java.util.Optional;

public interface EmployeeProfileQueryPort {
    Optional<EmployeeProfile> findById(Long employeeId);

    record EmployeeProfile(Long id, Long companyId, String name, String employeeCode,
            boolean mustChangePassword) {
    }
}
