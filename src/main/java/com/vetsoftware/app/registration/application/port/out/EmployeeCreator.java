package com.vetsoftware.app.registration.application.port.out;

public interface EmployeeCreator {
    EmployeeResult create(String employeeCode, String hashedPassword, String name,
                          String email, Long companyId);

    record EmployeeResult(Long id) {}
}
