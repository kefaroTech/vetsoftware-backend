package com.vetsoftware.app.procedureschedule.infrastructure.persistence;

import com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence.HospitalizationProcedureJpaRepository;
import com.vetsoftware.app.procedureschedule.application.port.out.HospitalizationProcedureQueryPort;
import com.vetsoftware.app.procedureschedule.domain.ProcedureOrderParams;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaHospitalizationProcedureQueryPort implements HospitalizationProcedureQueryPort {
  private final HospitalizationProcedureJpaRepository procedureJpaRepository;

  public JpaHospitalizationProcedureQueryPort(
      HospitalizationProcedureJpaRepository procedureJpaRepository) {
    this.procedureJpaRepository = procedureJpaRepository;
  }

  @Override
  public Optional<ProcedureOrderParams> findById(Long hospitalizationProcedureId) {
    return procedureJpaRepository
        .findById(hospitalizationProcedureId)
        .map(
            p ->
                new ProcedureOrderParams(
                    p.getId(),
                    p.getName(),
                    p.getHospitalization().getId(),
                    p.getFrequency(),
                    p.getGuidelineType(),
                    p.getDurationMeasure(),
                    p.getDurationQuantity(),
                    p.getStartDate(),
                    p.getStartTime()));
  }
}
