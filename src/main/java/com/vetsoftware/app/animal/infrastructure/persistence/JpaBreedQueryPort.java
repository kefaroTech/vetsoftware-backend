package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.BreedQueryPort;
import com.vetsoftware.app.animal.domain.BreedRef;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("animalJpaBreedQueryPort")
public class JpaBreedQueryPort implements BreedQueryPort {
  private final BreedJpaRepository breedJpaRepository;

  public JpaBreedQueryPort(BreedJpaRepository breedJpaRepository) {
    this.breedJpaRepository = breedJpaRepository;
  }

  @Override
  public Optional<BreedRef> findById(Long breedId) {
    return breedJpaRepository.findById(breedId).map(e -> new BreedRef(e.getId(), e.getName()));
  }
}
