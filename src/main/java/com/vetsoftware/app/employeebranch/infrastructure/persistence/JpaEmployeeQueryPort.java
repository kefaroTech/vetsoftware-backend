package com.vetsoftware.app.employeebranch.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.employeebranch.application.port.out.EmployeeQueryPort;
import org.springframework.stereotype.Component;

@Component("employeeBranchJpaEmployeeQueryPort")
public class JpaEmployeeQueryPort implements EmployeeQueryPort {

  private final EmployeeJpaRepository employeeJpaRepository;

  public JpaEmployeeQueryPort(EmployeeJpaRepository employeeJpaRepository) {
    this.employeeJpaRepository = employeeJpaRepository;
  }

  @Override
  public boolean existsByIdAndCompanyId(Long employeeId, Long companyId) {
    // findByIdAndCompany_Id lleva @SQLRestriction("enabled = true") → solo empleados activos de la
    // empresa.
    return employeeJpaRepository.findByIdAndCompany_Id(employeeId, companyId).isPresent();
  }
}
