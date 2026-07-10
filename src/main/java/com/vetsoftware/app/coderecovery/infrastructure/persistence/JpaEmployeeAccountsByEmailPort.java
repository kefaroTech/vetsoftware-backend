package com.vetsoftware.app.coderecovery.infrastructure.persistence;

import com.vetsoftware.app.coderecovery.application.port.out.EmployeeAccountsByEmailPort;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmployeeAccountsByEmailPort implements EmployeeAccountsByEmailPort {

    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaEmployeeAccountsByEmailPort(EmployeeJpaRepository employeeJpaRepository) {
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public List<EmployeeAccount> findByEmail(String email) {
        return employeeJpaRepository.findByEmailAndEmailVerified(email, true).stream()
            .map(e -> new EmployeeAccount(e.getName(), e.getEmployeeCode(), e.getCompany().getName()))
            .toList();
    }
}
