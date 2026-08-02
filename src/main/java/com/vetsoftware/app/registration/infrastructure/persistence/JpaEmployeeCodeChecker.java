package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.registration.application.port.out.EmployeeCodeChecker;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployeeCodeChecker implements EmployeeCodeChecker {
  private final EmployeeJpaRepository jpaRepository;

  public JpaEmployeeCodeChecker(EmployeeJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean exists(String employeeCode) {
    // Cuenta todas las filas (incluidas las de empleados desactivados) para que la disponibilidad
    // coincida con la constraint unique de la BD y no falle el INSERT posterior.
    return jpaRepository.countByEmployeeCodeAllRows(employeeCode) > 0;
  }
}
