package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import com.vetsoftware.app.debtopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("debtOpenAccountJpaEmployeeQueryPort")
public class JpaEmployeeQueryPort implements EmployeeQueryPort {
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaEmployeeQueryPort(EmployeeJpaRepository employeeJpaRepository) {
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public Optional<EmployeeRef> findById(Long employeeId) {
        return employeeJpaRepository.findById(employeeId)
            .map(e -> new EmployeeRef(e.getId(), e.getName()));
    }
}
