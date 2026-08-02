package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.diagnosticimaging.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.diagnosticimaging.domain.ConsultationRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("diagnosticImagingJpaConsultationQueryPort")
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
