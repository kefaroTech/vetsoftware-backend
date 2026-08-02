package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.AnimalColorQueryPort;
import com.vetsoftware.app.animal.domain.AnimalColorRef;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("animalJpaAnimalColorQueryPort")
public class JpaAnimalColorQueryPort implements AnimalColorQueryPort {
  private final AnimalColorJpaRepository animalColorJpaRepository;

  public JpaAnimalColorQueryPort(AnimalColorJpaRepository animalColorJpaRepository) {
    this.animalColorJpaRepository = animalColorJpaRepository;
  }

  @Override
  public Optional<AnimalColorRef> findById(Long colorId) {
    return animalColorJpaRepository
        .findById(colorId)
        .map(e -> new AnimalColorRef(e.getId(), e.getName()));
  }
}
