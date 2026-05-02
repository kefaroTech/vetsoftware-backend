package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.registration.application.port.out.EmployeeCodeChecker;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployeeCodeChecker implements EmployeeCodeChecker {
    private final EmployeeJpaRepository jpaRepository;

    public JpaEmployeeCodeChecker(EmployeeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean exists(String employeeCode) {
        return jpaRepository.existsByEmployeeCode(employeeCode);
    }
}
