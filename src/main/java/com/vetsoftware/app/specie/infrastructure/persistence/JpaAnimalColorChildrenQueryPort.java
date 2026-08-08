package com.vetsoftware.app.specie.infrastructure.persistence;

import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaRepository;
import com.vetsoftware.app.specie.application.port.out.AnimalColorChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaAnimalColorChildrenQueryPort implements AnimalColorChildrenQueryPort {
    private final AnimalColorJpaRepository jpaRepository;

    public JpaAnimalColorChildrenQueryPort(AnimalColorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveBySpecieId(Long parentId) {
        return jpaRepository.existsBySpecie_Id(parentId);
    }
}
