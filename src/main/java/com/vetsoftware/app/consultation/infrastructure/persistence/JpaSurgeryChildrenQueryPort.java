package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.consultation.application.port.out.SurgeryChildrenQueryPort;
import com.vetsoftware.app.surgery.infrastructure.persistence.SurgeryJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaSurgeryChildrenQueryPort implements SurgeryChildrenQueryPort {
    private final SurgeryJpaRepository jpaRepository;

    public JpaSurgeryChildrenQueryPort(SurgeryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByConsultationId(Long parentId) {
        return jpaRepository.existsByConsultation_Id(parentId);
    }
}
