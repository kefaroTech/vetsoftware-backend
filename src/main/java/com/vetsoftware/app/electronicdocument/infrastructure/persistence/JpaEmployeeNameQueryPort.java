package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.application.port.out.EmployeeNameQueryPort;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter del {@link EmployeeNameQueryPort}: lee el nombre del empleado de la feature employee. Único cruce
 * permitido de vertical slicing (persistence → persistence de otra feature).
 */
@Component
public class JpaEmployeeNameQueryPort implements EmployeeNameQueryPort {
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaEmployeeNameQueryPort(EmployeeJpaRepository employeeJpaRepository) {
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public Optional<String> findName(Long employeeId) {
        if (employeeId == null) return Optional.empty();
        return employeeJpaRepository.findById(employeeId)
                .map(EmployeeJpaEntity::getName)
                .filter(name -> name != null && !name.isBlank());
    }
}
