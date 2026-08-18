package com.vetsoftware.app.deworming.infrastructure.persistence;

import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.deworming.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.deworming.domain.ConsultationRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("dewormingJpaConsultationQueryPort")
public class JpaConsultationQueryPort implements ConsultationQueryPort {
    private final ConsultationJpaRepository consultationJpaRepository;

    public JpaConsultationQueryPort(ConsultationJpaRepository consultationJpaRepository) {
        this.consultationJpaRepository = consultationJpaRepository;
    }

    @Override
    public Optional<ConsultationRef> findByIdAndCompanyId(Long consultationId, Long companyId) {
        return consultationJpaRepository.findByIdAndCompany_Id(consultationId, companyId)
                .map(e -> new ConsultationRef(e.getId(), e.getDate()));
    }
}
