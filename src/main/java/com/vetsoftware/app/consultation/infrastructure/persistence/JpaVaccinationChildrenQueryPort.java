package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.consultation.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.vaccination.infrastructure.persistence.VaccinationJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaVaccinationChildrenQueryPort implements VaccinationChildrenQueryPort {
  private final VaccinationJpaRepository jpaRepository;

  public JpaVaccinationChildrenQueryPort(VaccinationJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByConsultationId(Long parentId) {
    return jpaRepository.existsByConsultation_Id(parentId);
  }
}
