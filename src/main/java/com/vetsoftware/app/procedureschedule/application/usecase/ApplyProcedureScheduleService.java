package com.vetsoftware.app.procedureschedule.application.usecase;

import com.vetsoftware.app.procedureschedule.application.command.ApplyProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.in.ApplyProcedureScheduleUseCase;
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

  public ApplyProcedureScheduleService(ProcedureScheduleRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public List<ProcedureScheduleDto> execute(ApplyProcedureScheduleCommand command) {
    ProcedureSchedule target =
        repository
            .findById(command.scheduleId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Procedure schedule not found: " + command.scheduleId()));
    target.apply(LocalDateTime.now()); // appliedStatus=APPLIED, realDateTime=now
    repository.save(target);

    // Pauta INTERVALO: aplicar tarde NO recalcula las siguientes; eso solo ocurre al
    // reprogramar una ejecución (drag&drop → reschedule mode=cascade).
    return repository
        .findByHospitalizationProcedureId(target.getHospitalizationProcedure().id())
        .stream()
        .map(ProcedureScheduleDto::from)
        .toList();
  }
}
