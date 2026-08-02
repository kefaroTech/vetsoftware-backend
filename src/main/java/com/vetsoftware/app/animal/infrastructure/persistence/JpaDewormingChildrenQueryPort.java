package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.DewormingChildrenQueryPort;
import com.vetsoftware.app.deworming.infrastructure.persistence.DewormingJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaDewormingChildrenQueryPort implements DewormingChildrenQueryPort {
  private final DewormingJpaRepository jpaRepository;

  public JpaDewormingChildrenQueryPort(DewormingJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByAnimalId(Long parentId) {
    return jpaRepository.existsByAnimal_Id(parentId);
  }
}
