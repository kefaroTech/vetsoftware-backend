package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.HospitalizationChildrenQueryPort;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaHospitalizationChildrenQueryPort implements HospitalizationChildrenQueryPort {
    private final HospitalizationJpaRepository jpaRepository;

    public JpaHospitalizationChildrenQueryPort(HospitalizationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByAnimalId(Long parentId) {
        return jpaRepository.existsByAnimal_Id(parentId);
    }
}
