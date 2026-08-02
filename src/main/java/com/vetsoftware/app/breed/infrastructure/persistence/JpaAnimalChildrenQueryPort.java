package com.vetsoftware.app.breed.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.breed.application.port.out.AnimalChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaAnimalChildrenQueryPort implements AnimalChildrenQueryPort {
  private final AnimalJpaRepository jpaRepository;

  public JpaAnimalChildrenQueryPort(AnimalJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByBreedId(Long parentId) {
    return jpaRepository.existsByBreed_Id(parentId);
  }
}
