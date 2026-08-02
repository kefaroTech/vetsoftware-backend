package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import com.vetsoftware.app.vaccination.infrastructure.persistence.VaccinationJpaRepository;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaVaccinationChildrenQueryPort implements VaccinationChildrenQueryPort {
  private final VaccinationJpaRepository jpaRepository;

  public JpaVaccinationChildrenQueryPort(VaccinationJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByVaccinationTypeId(Long parentId) {
    return jpaRepository.existsByVaccinationType_Id(parentId);
  }
}
