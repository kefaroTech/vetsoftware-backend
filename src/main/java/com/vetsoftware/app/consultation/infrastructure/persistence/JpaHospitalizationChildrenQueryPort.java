package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.consultation.application.port.out.HospitalizationChildrenQueryPort;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaHospitalizationChildrenQueryPort implements HospitalizationChildrenQueryPort {
    private final HospitalizationJpaRepository jpaRepository;

    public JpaHospitalizationChildrenQueryPort(HospitalizationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByConsultationId(Long parentId) {
        return jpaRepository.existsByConsultation_Id(parentId);
    }
}
