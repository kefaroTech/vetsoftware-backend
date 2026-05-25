package com.vetsoftware.app.animalcolor.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaAnimalChildrenQueryPort implements AnimalChildrenQueryPort {
    private final AnimalJpaRepository jpaRepository;

    public JpaAnimalChildrenQueryPort(AnimalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByAnimalColorId(Long parentId) {
        return jpaRepository.existsByColor_Id(parentId);
    }
}
