package com.vetsoftware.app.laboratorytesttype.infrastructure.persistence;

import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaRepository;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaLaboratoryTestChildrenQueryPort implements LaboratoryTestChildrenQueryPort {
  private final LaboratoryTestJpaRepository jpaRepository;

  public JpaLaboratoryTestChildrenQueryPort(LaboratoryTestJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByLaboratoryTestTypeId(Long parentId) {
    return jpaRepository.existsByTestType_Id(parentId);
  }
}
