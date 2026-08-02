package com.vetsoftware.app.passwordreset.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.passwordreset.application.port.out.EmployeeAccountLookupPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Busca la cuenta por código vía la persistencia de employee. {@code findByEmployeeCode} trae la
 * company por @EntityGraph y aplica @SQLRestriction("enabled = true"), así que solo devuelve
 * empleados activos.
 */
@Repository
public class JpaEmployeeAccountLookupPort implements EmployeeAccountLookupPort {

  private final EmployeeJpaRepository employeeJpaRepository;

  public JpaEmployeeAccountLookupPort(EmployeeJpaRepository employeeJpaRepository) {
    this.employeeJpaRepository = employeeJpaRepository;
  }

  @Override
  public Optional<EmployeeAccount> findByCode(String employeeCode) {
    return employeeJpaRepository
        .findByEmployeeCode(employeeCode)
        .map(
            e ->
                new EmployeeAccount(
                    e.getId(),
                    e.getCompany().getId(),
                    e.getName(),
                    e.getEmail(),
                    e.getCompany().getName(),
                    e.isEmailVerified()));
  }
}
