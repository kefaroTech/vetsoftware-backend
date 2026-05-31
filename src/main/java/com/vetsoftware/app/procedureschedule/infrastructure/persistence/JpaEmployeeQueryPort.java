package com.vetsoftware.app.procedureschedule.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.procedureschedule.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.procedureschedule.domain.EmployeeRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("procedureScheduleJpaEmployeeQueryPort")
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
