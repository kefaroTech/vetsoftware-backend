package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.EmployeeChildrenQueryPort;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployeeChildrenQueryPort implements EmployeeChildrenQueryPort {
  private final EmployeeJpaRepository jpaRepository;

  public JpaEmployeeChildrenQueryPort(EmployeeJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByCompanyId(Long parentId) {
    return jpaRepository.existsByCompany_Id(parentId);
  }
}
