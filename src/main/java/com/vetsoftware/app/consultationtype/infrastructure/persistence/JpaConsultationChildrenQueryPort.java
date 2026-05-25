package com.vetsoftware.app.consultationtype.infrastructure.persistence;

import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaConsultationChildrenQueryPort implements ConsultationChildrenQueryPort {
    private final ConsultationJpaRepository jpaRepository;

    public JpaConsultationChildrenQueryPort(ConsultationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByConsultationTypeId(Long parentId) {
        return jpaRepository.existsByConsultationType_Id(parentId);
    }
}
