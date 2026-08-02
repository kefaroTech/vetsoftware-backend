package com.vetsoftware.app.appointment.infrastructure.persistence;

import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("appointmentJpaEmployeeQueryPort")
public class JpaEmployeeQueryPort implements EmployeeQueryPort {
  private final EmployeeJpaRepository employeeJpaRepository;

  public JpaEmployeeQueryPort(EmployeeJpaRepository employeeJpaRepository) {
    this.employeeJpaRepository = employeeJpaRepository;
  }

  @Override
  public Optional<EmployeeRef> findByIdAndCompanyId(Long employeeId, Long companyId) {
    return employeeJpaRepository
        .findByIdAndCompany_Id(employeeId, companyId)
        .map(e -> new EmployeeRef(e.getId(), e.getName()));
  }
}
