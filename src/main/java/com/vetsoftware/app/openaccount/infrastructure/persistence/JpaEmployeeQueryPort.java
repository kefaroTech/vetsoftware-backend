package com.vetsoftware.app.openaccount.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.openaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("openAccountJpaEmployeeQueryPort")
public class JpaEmployeeQueryPort implements EmployeeQueryPort {
  private final EmployeeJpaRepository employeeJpaRepository;

  public JpaEmployeeQueryPort(EmployeeJpaRepository employeeJpaRepository) {
    this.employeeJpaRepository = employeeJpaRepository;
  }

  @Override
  public Optional<EmployeeRef> findById(Long employeeId) {
    return employeeJpaRepository
        .findById(employeeId)
        .map(e -> new EmployeeRef(e.getId(), e.getName()));
  }
}
