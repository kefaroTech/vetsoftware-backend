package com.vetsoftware.app.employee.application.port.out;

public interface EmployeePasswordHasherPort {
    String hash(String rawPassword);
}
