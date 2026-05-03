package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.EmployeeCredentialsRepository;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmployeeCredentialsRepository implements EmployeeCredentialsRepository {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaEmployeeCredentialsRepository(EmployeeJpaRepository employeeJpaRepository) {
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public Optional<EmployeeCredentials> findByCode(String employeeCode) {
        return employeeJpaRepository.findByEmployeeCodeAndStatus(employeeCode, ACTIVE_STATUS)
                .map(e -> new EmployeeCredentials(e.getId(), e.getCompany().getId(), e.getHashPassword()));
    }
}
