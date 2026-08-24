package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.subscription.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.subscription.domain.EmployeeRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resuelve al empleado que firma el otrosi <strong>acotado por
 * empresa</strong>. Es R14 hecha codigo: la FK es simple, asi que si esto no
 * filtra, un otrosi de la clinica A lo puede firmar un empleado de la B y la
 * base lo acepta.
 */
@Component("subscriptionJpaEmployeeQueryPort")
public class JpaEmployeeQueryPort implements EmployeeQueryPort {

    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaEmployeeQueryPort(EmployeeJpaRepository employeeJpaRepository) {
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public Optional<EmployeeRef> findByIdAndCompanyId(Long employeeId, Long companyId) {
        if (employeeId == null || companyId == null)
            return Optional.empty();
        return employeeJpaRepository.findById(employeeId)
                .filter(e -> e.getCompany() != null && companyId.equals(e.getCompany().getId()))
                .map(e -> new EmployeeRef(e.getId(), e.getName()));
    }
}
