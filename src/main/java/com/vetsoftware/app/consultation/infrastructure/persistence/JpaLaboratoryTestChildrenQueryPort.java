package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.consultation.application.port.out.LaboratoryTestChildrenQueryPort;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaLaboratoryTestChildrenQueryPort implements LaboratoryTestChildrenQueryPort {
    private final LaboratoryTestJpaRepository jpaRepository;

    public JpaLaboratoryTestChildrenQueryPort(LaboratoryTestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByConsultationId(Long parentId) {
        return jpaRepository.existsByConsultation_Id(parentId);
    }
}
