package com.vetsoftware.app.procedureschedule.application.port.out;

import com.vetsoftware.app.procedureschedule.domain.ProcedureOrderParams;
import java.util.Optional;

public interface HospitalizationProcedureQueryPort {
  Optional<ProcedureOrderParams> findById(Long hospitalizationProcedureId);
}
