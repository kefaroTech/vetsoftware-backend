package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.laboratorytest.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.laboratorytest.domain.AnimalRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("laboratoryTestJpaAnimalQueryPort")
public class JpaAnimalQueryPort implements AnimalQueryPort {
    private final AnimalJpaRepository animalJpaRepository;

    public JpaAnimalQueryPort(AnimalJpaRepository animalJpaRepository) {
        this.animalJpaRepository = animalJpaRepository;
    }

    @Override
    public Optional<AnimalRef> findById(Long animalId) {
        return animalJpaRepository.findById(animalId)
            .map(e -> new AnimalRef(e.getId(), e.getName(), e.getCode()));
    }
}
