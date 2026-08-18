package com.vetsoftware.app.procedureschedule.application.usecase;

import com.vetsoftware.app.procedureschedule.application.command.RescheduleProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.in.RescheduleProcedureScheduleUseCase;
import com.vetsoftware.app.procedureschedule.application.port.out.HospitalizationProcedureQueryPort;
import com.vetsoftware.app.procedureschedule.application.port.out.ProcedureScheduleRepository;
import com.vetsoftware.app.procedureschedule.domain.AppliedStatus;
import com.vetsoftware.app.procedureschedule.domain.ProcedureOrderParams;
import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import com.vetsoftware.app.procedureschedule.domain.ProcedureScheduleGenerator;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "procedure.schedule.reschedule")
@Service
public class RescheduleProcedureScheduleService implements RescheduleProcedureScheduleUseCase {
    private final ProcedureScheduleRepository repository;
    private final HospitalizationProcedureQueryPort procedureQueryPort;

    public RescheduleProcedureScheduleService(ProcedureScheduleRepository repository,
            HospitalizationProcedureQueryPort procedureQueryPort) {
        this.repository = repository;
        this.procedureQueryPort = procedureQueryPort;
    }

    /**
     * La ejecucion no tiene empresa propia, asi que la propiedad se comprueba
     * subiendo a la orden de procedimiento y de ahi a la hospitalizacion, que si la
     * tiene. El chequeo va <em>antes</em> del primer {@code reschedule}/
     * {@code save}: en modo cascada esto no movia una fila sino toda la pauta
     * pendiente de un paciente de otro tenant.
     *
     * <p>
     * Una vez validada la orden, el resto del plan es suyo por construccion: todas
     * las ejecuciones cuelgan de la misma orden. {@code companyId == null} es el
     * camino SYSTEM.
     */
    @Override
    @Transactional
    public List<ProcedureScheduleDto> execute(RescheduleProcedureScheduleCommand command) {
        if (command.newDateTime() == null)
            throw new IllegalArgumentException("newDateTime is required");

        ProcedureSchedule probe = repository.findById(command.scheduleId())
                .orElseThrow(() -> notFound(command.scheduleId()));
        Long procedureId = probe.getHospitalizationProcedure().id();
        ProcedureOrderParams owned = requireOwnedByCompany(procedureId, command.companyId(),
                command.scheduleId());

        // Orden previo al movimiento (define "las siguientes").
        List<ProcedureSchedule> all = new ArrayList<>(command.companyId() == null
                ? repository.findByHospitalizationProcedureId(procedureId)
                : repository.findByHospitalizationProcedureIdAndCompanyId(procedureId,
                        command.companyId()));
        all.sort(Comparator.comparing(ProcedureSchedule::getCurrentDateTime));
        int idx = indexOfId(all, command.scheduleId());

        ProcedureSchedule target = all.get(idx);
        target.reschedule(command.newDateTime());
        repository.save(target);

        if ("cascade".equalsIgnoreCase(command.mode())) {
            ProcedureOrderParams params = owned != null
                    ? owned
                    : procedureQueryPort.findById(procedureId).orElse(null);
            if (params != null && "INTERVAL".equalsIgnoreCase(params.guidelineType())) {
                Integer interval = ProcedureScheduleGenerator.intervalHours(params.frequency());
                if (interval != null)
                    recalcFollowing(all, idx, command.newDateTime(), interval);
            }
        }

        return all.stream().map(ProcedureScheduleDto::from).toList();
    }

    /**
     * Devuelve la orden ya resuelta para no repetir la consulta en el modo cascada.
     * {@code null} solo en el camino SYSTEM, donde no hay empresa que acotar y la
     * orden se resuelve mas tarde si hace falta. Mismo mensaje que el id
     * inexistente: a un tenant ajeno no se le confirma que la ejecucion existe.
     */
    private ProcedureOrderParams requireOwnedByCompany(Long procedureId, Long companyId,
            Long scheduleId) {
        if (companyId == null) {
            return null;
        }
        return procedureQueryPort.findByIdAndCompanyId(procedureId, companyId)
                .orElseThrow(() -> notFound(scheduleId));
    }

    private static IllegalArgumentException notFound(Long scheduleId) {
        return new IllegalArgumentException("Procedure schedule not found: " + scheduleId);
    }

    private static int indexOfId(List<ProcedureSchedule> all, Long id) {
        for (int i = 0; i < all.size(); i++) {
            if (id.equals(all.get(i).getId()))
                return i;
        }
        throw new IllegalArgumentException("Procedure schedule not found in plan: " + id);
    }

    private void recalcFollowing(List<ProcedureSchedule> all, int pivotIdx, LocalDateTime from,
            int intervalHours) {
        LocalDateTime cursor = from;
        for (int i = pivotIdx + 1; i < all.size(); i++) {
            ProcedureSchedule s = all.get(i);
            if (s.getAppliedStatus() != AppliedStatus.PENDING)
                continue;
            cursor = cursor.plusHours(intervalHours);
            s.reschedule(cursor);
            repository.save(s);
        }
    }
}
