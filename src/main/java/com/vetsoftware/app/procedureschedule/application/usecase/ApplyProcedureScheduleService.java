package com.vetsoftware.app.procedureschedule.application.usecase;

import com.vetsoftware.app.procedureschedule.application.command.ApplyProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.in.ApplyProcedureScheduleUseCase;
import com.vetsoftware.app.procedureschedule.application.port.out.HospitalizationProcedureQueryPort;
import com.vetsoftware.app.procedureschedule.application.port.out.ProcedureScheduleRepository;
import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "procedure.schedule.apply")
@Service
public class ApplyProcedureScheduleService implements ApplyProcedureScheduleUseCase {
    private final ProcedureScheduleRepository repository;
    private final HospitalizationProcedureQueryPort procedureQueryPort;

    public ApplyProcedureScheduleService(ProcedureScheduleRepository repository,
            HospitalizationProcedureQueryPort procedureQueryPort) {
        this.repository = repository;
        this.procedureQueryPort = procedureQueryPort;
    }

    /**
     * La ejecucion no tiene empresa propia, asi que la propiedad se comprueba
     * subiendo a la orden de procedimiento y de ahi a la hospitalizacion, que si la
     * tiene. Sin esa comprobacion, cualquiera con {@code hospitalization.update}
     * marcaba como APLICADA la ejecucion de un paciente de otro tenant adivinando
     * el id, y la hoja ajena quedaba falseada.
     *
     * <p>
     * El chequeo va <em>antes</em> de {@code apply} y de {@code save}: si falla, no
     * se ha escrito nada. {@code companyId == null} es el camino SYSTEM.
     */
    @Override
    @Transactional
    public List<ProcedureScheduleDto> execute(ApplyProcedureScheduleCommand command) {
        ProcedureSchedule target = repository.findById(command.scheduleId())
                .orElseThrow(() -> notFound(command.scheduleId()));
        Long procedureId = target.getHospitalizationProcedure().id();
        requireOwnedByCompany(procedureId, command.companyId(), command.scheduleId());

        target.apply(LocalDateTime.now()); // appliedStatus=APPLIED, realDateTime=now
        repository.save(target);

        // Pauta INTERVALO: aplicar tarde NO recalcula las siguientes; eso solo ocurre
        // al
        // reprogramar una ejecución (drag&drop → reschedule mode=cascade).
        return (command.companyId() == null
                ? repository.findByHospitalizationProcedureId(procedureId)
                : repository.findByHospitalizationProcedureIdAndCompanyId(procedureId,
                        command.companyId()))
                .stream().map(ProcedureScheduleDto::from).toList();
    }

    /**
     * Mismo mensaje que el id inexistente: a un tenant ajeno no se le confirma que
     * la ejecucion existe.
     */
    private void requireOwnedByCompany(Long procedureId, Long companyId, Long scheduleId) {
        if (companyId == null) {
            return;
        }
        procedureQueryPort.findByIdAndCompanyId(procedureId, companyId)
                .orElseThrow(() -> notFound(scheduleId));
    }

    private static IllegalArgumentException notFound(Long scheduleId) {
        return new IllegalArgumentException("Procedure schedule not found: " + scheduleId);
    }
}
