package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.EmployeeCredentialsRepository;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmployeeCredentialsRepository implements EmployeeCredentialsRepository {

  private final EmployeeJpaRepository employeeJpaRepository;

  public JpaEmployeeCredentialsRepository(EmployeeJpaRepository employeeJpaRepository) {
    this.employeeJpaRepository = employeeJpaRepository;
  }

  @Override
  public Optional<EmployeeCredentials> findByCode(String employeeCode) {
    return employeeJpaRepository
        .findByEmployeeCode(employeeCode)
        .map(
            e ->
                new EmployeeCredentials(
                    e.getId(),
                    e.getCompany().getId(),
                    e.getAuthVersion(),
                    e.getHashPassword(),
                    e.isEmailVerified()));
  }
}
