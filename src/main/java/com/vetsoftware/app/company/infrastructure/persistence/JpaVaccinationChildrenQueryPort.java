package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.vaccination.infrastructure.persistence.VaccinationJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaVaccinationChildrenQueryPort implements VaccinationChildrenQueryPort {
  private final VaccinationJpaRepository jpaRepository;

  public JpaVaccinationChildrenQueryPort(VaccinationJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByCompanyId(Long parentId) {
    return jpaRepository.existsByCompany_Id(parentId);
  }
}
