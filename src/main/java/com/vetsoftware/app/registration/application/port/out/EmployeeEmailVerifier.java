package com.vetsoftware.app.registration.application.port.out;

/** Puerto hacia la feature employee para marcar un correo como verificado. */
public interface EmployeeEmailVerifier {
    void verify(Long employeeId);
}
