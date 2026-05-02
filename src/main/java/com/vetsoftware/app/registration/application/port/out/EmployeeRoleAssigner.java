package com.vetsoftware.app.registration.application.port.out;

public interface EmployeeRoleAssigner {
    void assign(Long employeeId, Long roleId);
}
