package com.vetsoftware.app.employeerole.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.employeerole.domain.EmployeeRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("employeeroleJpaEmployeeQueryPort")
public class JpaEmployeeQueryPort implements EmployeeQueryPort {
  private final EmployeeJpaRepository employeeJpaRepository;

  public JpaEmployeeQueryPort(EmployeeJpaRepository employeeJpaRepository) {
    this.employeeJpaRepository = employeeJpaRepository;
  }

  @Override
  public Optional<EmployeeRef> findById(Long employeeId) {
    return employeeJpaRepository
        .findById(employeeId)
        .map(e -> new EmployeeRef(e.getId(), e.getEmployeeCode(), e.getName()));
  }
}
