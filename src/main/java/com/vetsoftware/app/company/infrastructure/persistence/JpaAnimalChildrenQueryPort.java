package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.application.port.out.AnimalChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaAnimalChildrenQueryPort implements AnimalChildrenQueryPort {
  private final AnimalJpaRepository jpaRepository;

  public JpaAnimalChildrenQueryPort(AnimalJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByCompanyId(Long parentId) {
    return jpaRepository.existsByCompany_Id(parentId);
  }
}
