package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.consultation.application.port.out.PrescriptionChildrenQueryPort;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaPrescriptionChildrenQueryPort implements PrescriptionChildrenQueryPort {
    private final PrescriptionJpaRepository jpaRepository;

    public JpaPrescriptionChildrenQueryPort(PrescriptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByConsultationId(Long parentId) {
        return jpaRepository.existsByConsultation_Id(parentId);
    }
}
