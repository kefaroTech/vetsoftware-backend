package com.vetsoftware.app.procedureschedule.application.port.out;

import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import java.util.List;
import java.util.Optional;

public interface ProcedureScheduleRepository {
    ProcedureSchedule save(ProcedureSchedule procedureSchedule);

    Optional<ProcedureSchedule> findById(Long id);

    List<ProcedureSchedule> findByHospitalizationProcedureId(Long hospitalizationProcedureId);

    /**
     * Las ejecuciones del procedimiento, solo si el procedimiento es de
     * {@code companyId}. La empresa no cuelga de la ejecución: sube por el
     * procedimiento hasta la hospitalización.
     */
    List<ProcedureSchedule> findByHospitalizationProcedureIdAndCompanyId(
            Long hospitalizationProcedureId, Long companyId);

    List<ProcedureSchedule> findByHospitalizationId(Long hospitalizationId);

    /**
     * Las ejecuciones de la hospitalización, solo si la hospitalización es de
     * {@code companyId}.
     */
    List<ProcedureSchedule> findByHospitalizationIdAndCompanyId(Long hospitalizationId,
            Long companyId);

    void delete(Long id);

    /**
     * Sin acotar: solo el camino SYSTEM ({@code companyId == null}).
     */
    void disableByHospitalizationProcedureId(Long hospitalizationProcedureId);

    /**
     * Acotado al tenant: el {@code EXISTS} sube del procedimiento a la
     * hospitalización.
     */
    void disableByHospitalizationProcedureId(Long hospitalizationProcedureId, Long companyId);

    /**
     * Deshabilita solo las ejecuciones NO aplicadas (conserva el histórico de las
     * aplicadas).
     */
    void disablePendingByHospitalizationProcedureId(Long hospitalizationProcedureId);

    /**
     * Igual que {@link #disablePendingByHospitalizationProcedureId(Long)} pero
     * acotado al tenant. Aquí no hay lectura previa que valide la propiedad —el
     * servicio decide qué devolver mirando lo que quedó vivo—, así que este filtro
     * es la única barrera.
     */
    void disablePendingByHospitalizationProcedureId(Long hospitalizationProcedureId,
            Long companyId);
}
