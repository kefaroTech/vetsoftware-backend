package com.vetsoftware.app.specie.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.specie.application.port.out.AnimalChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaAnimalChildrenQueryPort implements AnimalChildrenQueryPort {
  private final AnimalJpaRepository jpaRepository;

  public JpaAnimalChildrenQueryPort(AnimalJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveBySpecieId(Long parentId) {
    return jpaRepository.existsBySpecie_Id(parentId);
  }
}
