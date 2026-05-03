package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.consultation.application.port.out.ConsultationTypeQueryPort;
import com.vetsoftware.app.consultation.domain.ConsultationTypeRef;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("consultationJpaConsultationTypeQueryPort")
public class JpaConsultationTypeQueryPort implements ConsultationTypeQueryPort {
    private final ConsultationTypeJpaRepository consultationTypeJpaRepository;

    public JpaConsultationTypeQueryPort(ConsultationTypeJpaRepository consultationTypeJpaRepository) {
        this.consultationTypeJpaRepository = consultationTypeJpaRepository;
    }

    @Override
    public Optional<ConsultationTypeRef> findById(Long consultationTypeId) {
        return consultationTypeJpaRepository.findById(consultationTypeId)
            .map(e -> new ConsultationTypeRef(e.getId(), e.getName()));
    }
}
