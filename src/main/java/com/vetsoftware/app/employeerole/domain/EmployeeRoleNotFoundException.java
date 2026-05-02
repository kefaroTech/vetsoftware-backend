package com.vetsoftware.app.employeerole.domain;

public class EmployeeRoleNotFoundException extends RuntimeException {
    public EmployeeRoleNotFoundException(Long id) {
        super("EmployeeRole not found: " + id);
    }
}
