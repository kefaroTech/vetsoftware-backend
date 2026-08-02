package com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence;

import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationQueryPort;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("hospitalizationProcedureJpaHospitalizationQueryPort")
public class JpaHospitalizationQueryPort implements HospitalizationQueryPort {
  private final HospitalizationJpaRepository hospitalizationJpaRepository;

  public JpaHospitalizationQueryPort(HospitalizationJpaRepository hospitalizationJpaRepository) {
    this.hospitalizationJpaRepository = hospitalizationJpaRepository;
  }

  @Override
  public Optional<HospitalizationRef> findById(Long hospitalizationId) {
    return hospitalizationJpaRepository
        .findById(hospitalizationId)
        .map(e -> new HospitalizationRef(e.getId(), e.getDate()));
  }
}
