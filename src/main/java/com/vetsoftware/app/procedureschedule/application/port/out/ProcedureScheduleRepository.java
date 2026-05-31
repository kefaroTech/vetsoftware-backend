package com.vetsoftware.app.procedureschedule.application.port.out;

import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import java.util.List;
import java.util.Optional;

public interface ProcedureScheduleRepository {
    ProcedureSchedule save(ProcedureSchedule procedureSchedule);
    Optional<ProcedureSchedule> findById(Long id);
    List<ProcedureSchedule> findByHospitalizationProcedureId(Long hospitalizationProcedureId);
    void delete(Long id);
    int reactivate(Long id);
}
