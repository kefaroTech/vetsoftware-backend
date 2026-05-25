package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.DayCareChildrenQueryPort;
import com.vetsoftware.app.daycare.infrastructure.persistence.DayCareJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaDayCareChildrenQueryPort implements DayCareChildrenQueryPort {
    private final DayCareJpaRepository jpaRepository;

    public JpaDayCareChildrenQueryPort(DayCareJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByAnimalId(Long parentId) {
        return jpaRepository.existsByAnimal_Id(parentId);
    }
}
