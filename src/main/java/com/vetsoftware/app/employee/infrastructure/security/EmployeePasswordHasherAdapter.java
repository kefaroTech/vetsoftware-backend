package com.vetsoftware.app.employee.infrastructure.security;

import com.vetsoftware.app.employee.application.port.out.EmployeePasswordHasherPort;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import org.springframework.stereotype.Component;

@Component
public class EmployeePasswordHasherAdapter implements EmployeePasswordHasherPort {

    private final PasswordHasher passwordHasher;

    public EmployeePasswordHasherAdapter(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordHasher.hash(rawPassword);
    }
}
