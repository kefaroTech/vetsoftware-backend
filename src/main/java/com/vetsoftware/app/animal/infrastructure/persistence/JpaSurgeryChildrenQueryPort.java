package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.SurgeryChildrenQueryPort;
import com.vetsoftware.app.surgery.infrastructure.persistence.SurgeryJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaSurgeryChildrenQueryPort implements SurgeryChildrenQueryPort {
    private final SurgeryJpaRepository jpaRepository;

    public JpaSurgeryChildrenQueryPort(SurgeryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByAnimalId(Long parentId) {
        return jpaRepository.existsByAnimal_Id(parentId);
    }
}
