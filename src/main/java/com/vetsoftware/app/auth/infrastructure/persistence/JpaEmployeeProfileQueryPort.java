package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.EmployeeProfileQueryPort;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmployeeProfileQueryPort implements EmployeeProfileQueryPort {

    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaEmployeeProfileQueryPort(EmployeeJpaRepository employeeJpaRepository) {
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public Optional<EmployeeProfile> findById(Long employeeId) {
        return employeeJpaRepository.findById(employeeId)
                .map(e -> new EmployeeProfile(e.getId(), e.getCompany().getId(), e.getName(),
                        e.getEmployeeCode(), e.isMustChangePassword()));
    }
}
