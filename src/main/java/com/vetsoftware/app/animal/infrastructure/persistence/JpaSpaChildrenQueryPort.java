package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.SpaChildrenQueryPort;
import com.vetsoftware.app.spa.infrastructure.persistence.SpaJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaSpaChildrenQueryPort implements SpaChildrenQueryPort {
    private final SpaJpaRepository jpaRepository;

    public JpaSpaChildrenQueryPort(SpaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByAnimalId(Long parentId) {
        return jpaRepository.existsByAnimal_Id(parentId);
    }
}
