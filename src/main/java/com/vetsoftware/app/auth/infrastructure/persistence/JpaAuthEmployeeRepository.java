package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAuthEmployeeRepository implements AuthEmployeeRepository {

    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaAuthEmployeeRepository(EmployeeJpaRepository employeeJpaRepository) {
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public Optional<AuthEmployee> findActiveById(Long employeeId) {
        return employeeJpaRepository.findActiveWithCompanyById(employeeId)
                .map(e -> new AuthEmployee(e.getId(), e.getCompany().getId(), e.getAuthVersion()));
    }

    @Override
    public Optional<AuthEmployee> rotateAuthVersion(Long employeeId) {
        return employeeJpaRepository.findActiveWithCompanyByIdForUpdate(employeeId)
                .map(employee -> {
                    long nextVersion = employee.getAuthVersion() + 1L;
                    employee.setAuthVersion(nextVersion);
                    employeeJpaRepository.saveAndFlush(employee);
                    return new AuthEmployee(employee.getId(), employee.getCompany().getId(),
                            nextVersion);
                });
    }

    @Override
    public void bumpAuthVersion(Long employeeId) {
        employeeJpaRepository.bumpAuthVersion(employeeId);
    }
}
