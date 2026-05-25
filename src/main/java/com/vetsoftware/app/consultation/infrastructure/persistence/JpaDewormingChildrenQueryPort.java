package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.consultation.application.port.out.DewormingChildrenQueryPort;
import com.vetsoftware.app.deworming.infrastructure.persistence.DewormingJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaDewormingChildrenQueryPort implements DewormingChildrenQueryPort {
    private final DewormingJpaRepository jpaRepository;

    public JpaDewormingChildrenQueryPort(DewormingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByConsultationId(Long parentId) {
        return jpaRepository.existsByConsultation_Id(parentId);
    }
}
