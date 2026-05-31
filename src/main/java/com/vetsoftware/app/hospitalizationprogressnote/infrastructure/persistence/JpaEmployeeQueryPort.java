package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationprogressnote.domain.EmployeeRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("hospitalizationProgressNoteJpaEmployeeQueryPort")
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
