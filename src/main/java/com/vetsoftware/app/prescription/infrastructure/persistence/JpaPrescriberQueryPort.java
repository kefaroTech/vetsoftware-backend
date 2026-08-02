package com.vetsoftware.app.prescription.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.prescription.application.port.out.PrescriberQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Resuelve el nombre del profesional firmante desde el empleado autenticado. */
@Component
public class JpaPrescriberQueryPort implements PrescriberQueryPort {

  private final EmployeeJpaRepository employeeJpaRepository;

  public JpaPrescriberQueryPort(EmployeeJpaRepository employeeJpaRepository) {
    this.employeeJpaRepository = employeeJpaRepository;
  }

  @Override
  public Optional<String> findName(Long employeeId) {
    return employeeJpaRepository.findById(employeeId).map(e -> e.getName());
  }
}
