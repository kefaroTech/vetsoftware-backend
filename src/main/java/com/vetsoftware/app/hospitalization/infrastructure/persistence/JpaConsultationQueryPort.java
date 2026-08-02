package com.vetsoftware.app.hospitalization.infrastructure.persistence;

import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.hospitalization.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.hospitalization.domain.ConsultationRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("hospitalizationJpaConsultationQueryPort")
public class JpaConsultationQueryPort implements ConsultationQueryPort {
  private final ConsultationJpaRepository consultationJpaRepository;

  public JpaConsultationQueryPort(ConsultationJpaRepository consultationJpaRepository) {
    this.consultationJpaRepository = consultationJpaRepository;
  }

  @Override
  public Optional<ConsultationRef> findById(Long consultationId) {
    return consultationJpaRepository
        .findById(consultationId)
        .map(e -> new ConsultationRef(e.getId(), e.getDate()));
  }
}
