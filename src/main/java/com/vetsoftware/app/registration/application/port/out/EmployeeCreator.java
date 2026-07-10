package com.vetsoftware.app.registration.application.port.out;

public interface EmployeeCreator {
    // rawPassword: la contraseña SIN hashear. El adapter la delega a CreateEmployee, que la hashea una vez.
    EmployeeResult create(String employeeCode, String rawPassword, String name,
                          String email, Long companyId);

    record EmployeeResult(Long id) {}
}
