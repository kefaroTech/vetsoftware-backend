package com.vetsoftware.app.procedureschedule.application.port.out;

import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import java.util.List;
import java.util.Optional;

public interface ProcedureScheduleRepository {
    ProcedureSchedule save(ProcedureSchedule procedureSchedule);
    Optional<ProcedureSchedule> findById(Long id);
    List<ProcedureSchedule> findByHospitalizationProcedureId(Long hospitalizationProcedureId);
    List<ProcedureSchedule> findByHospitalizationId(Long hospitalizationId);
    void delete(Long id);
    void disableByHospitalizationProcedureId(Long hospitalizationProcedureId);
    /** Deshabilita solo las ejecuciones NO aplicadas (conserva el histórico de las aplicadas). */
    void disablePendingByHospitalizationProcedureId(Long hospitalizationProcedureId);
    int reactivate(Long id);
}
