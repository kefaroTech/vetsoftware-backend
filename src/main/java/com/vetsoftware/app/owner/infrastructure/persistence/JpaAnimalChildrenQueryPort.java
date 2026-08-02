package com.vetsoftware.app.owner.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.owner.application.port.out.AnimalChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaAnimalChildrenQueryPort implements AnimalChildrenQueryPort {
  private final AnimalJpaRepository jpaRepository;

  public JpaAnimalChildrenQueryPort(AnimalJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByOwnerId(Long parentId) {
    return jpaRepository.existsByOwner_Id(parentId);
  }
}
