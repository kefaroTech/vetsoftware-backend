package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.vaccination.infrastructure.persistence.VaccinationJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaVaccinationChildrenQueryPort implements VaccinationChildrenQueryPort {
  private final VaccinationJpaRepository jpaRepository;

  public JpaVaccinationChildrenQueryPort(VaccinationJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByAnimalId(Long parentId) {
    return jpaRepository.existsByAnimal_Id(parentId);
  }
}
