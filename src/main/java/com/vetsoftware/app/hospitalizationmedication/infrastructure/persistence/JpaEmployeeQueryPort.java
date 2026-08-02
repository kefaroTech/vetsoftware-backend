package com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationmedication.domain.EmployeeRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("hospitalizationMedicationJpaEmployeeQueryPort")
public class JpaEmployeeQueryPort implements EmployeeQueryPort {
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaEmployeeQueryPort(EmployeeJpaRepository employeeJpaRepository) {
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public Optional<EmployeeRef> findById(Long employeeId) {
        return employeeJpaRepository.findById(employeeId)
                .map(e -> new EmployeeRef(e.getId(), e.getEmployeeCode(), e.getName()));
    }
}
