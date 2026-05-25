package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.LaboratoryTestChildrenQueryPort;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaLaboratoryTestChildrenQueryPort implements LaboratoryTestChildrenQueryPort {
    private final LaboratoryTestJpaRepository jpaRepository;

    public JpaLaboratoryTestChildrenQueryPort(LaboratoryTestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByAnimalId(Long parentId) {
        return jpaRepository.existsByAnimal_Id(parentId);
    }
}
