package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("productChargeOpenAccountJpaEmployeeQueryPort")
public class JpaEmployeeQueryPort implements EmployeeQueryPort {
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaEmployeeQueryPort(EmployeeJpaRepository employeeJpaRepository) {
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public Optional<EmployeeRef> findByIdAndCompanyId(Long employeeId, Long companyId) {
        return employeeJpaRepository.findByIdAndCompany_Id(employeeId, companyId)
            .map(e -> new EmployeeRef(e.getId(), e.getName()));
    }
}
