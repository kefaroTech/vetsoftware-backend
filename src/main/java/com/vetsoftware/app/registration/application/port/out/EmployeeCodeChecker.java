package com.vetsoftware.app.registration.application.port.out;

public interface EmployeeCodeChecker {
    boolean exists(String employeeCode);
}
