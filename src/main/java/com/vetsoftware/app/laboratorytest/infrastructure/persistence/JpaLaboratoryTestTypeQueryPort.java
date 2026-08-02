package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestTypeQueryPort;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestTypeRef;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("laboratoryTestJpaLaboratoryTestTypeQueryPort")
public class JpaLaboratoryTestTypeQueryPort implements LaboratoryTestTypeQueryPort {
  private final LaboratoryTestTypeJpaRepository testTypeJpaRepository;

  public JpaLaboratoryTestTypeQueryPort(LaboratoryTestTypeJpaRepository testTypeJpaRepository) {
    this.testTypeJpaRepository = testTypeJpaRepository;
  }

  @Override
  public Optional<LaboratoryTestTypeRef> findById(Long testTypeId) {
    return testTypeJpaRepository
        .findById(testTypeId)
        .map(e -> new LaboratoryTestTypeRef(e.getId(), e.getName()));
  }
}
