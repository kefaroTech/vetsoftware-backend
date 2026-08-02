package com.vetsoftware.app.specie.infrastructure.persistence;

import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaRepository;
import com.vetsoftware.app.specie.application.port.out.BreedChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaBreedChildrenQueryPort implements BreedChildrenQueryPort {
  private final BreedJpaRepository jpaRepository;

  public JpaBreedChildrenQueryPort(BreedJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveBySpecieId(Long parentId) {
    return jpaRepository.existsBySpecie_Id(parentId);
  }
}
