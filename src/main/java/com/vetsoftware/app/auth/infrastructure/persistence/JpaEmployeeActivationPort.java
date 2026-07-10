package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.EmployeeActivationPort;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmployeeActivationPort implements EmployeeActivationPort {

    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaEmployeeActivationPort(EmployeeJpaRepository employeeJpaRepository) {
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public void activateOnLogin(Long employeeId) {
        employeeJpaRepository.activateInvited(employeeId);
    }
}
