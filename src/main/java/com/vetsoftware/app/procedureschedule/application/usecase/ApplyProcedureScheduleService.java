package com.vetsoftware.app.procedureschedule.application.usecase;

import com.vetsoftware.app.procedureschedule.application.command.ApplyProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.in.ApplyProcedureScheduleUseCase;
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

@Observed(name = "procedure_schedule.apply")
@Service
public class ApplyProcedureScheduleService implements ApplyProcedureScheduleUseCase {
    private final ProcedureScheduleRepository repository;
    private final HospitalizationProcedureQueryPort procedureQueryPort;

    public ApplyProcedureScheduleService(ProcedureScheduleRepository repository,
                                         HospitalizationProcedureQueryPort procedureQueryPort) {
        this.repository = repository;
        this.procedureQueryPort = procedureQueryPort;
    }

    @Override
    @Transactional
    public List<ProcedureScheduleDto> execute(ApplyProcedureScheduleCommand command) {
        ProcedureSchedule probe = repository.findById(command.scheduleId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Procedure schedule not found: " + command.scheduleId()));
        Long procedureId = probe.getHospitalizationProcedure().id();

        List<ProcedureSchedule> all = new ArrayList<>(repository.findByHospitalizationProcedureId(procedureId));
        all.sort(Comparator.comparing(ProcedureSchedule::getCurrentDateTime));
        int idx = indexOfId(all, command.scheduleId());

        LocalDateTime realTime = LocalDateTime.now();
        ProcedureSchedule target = all.get(idx);
        target.apply(realTime);
        repository.save(target);

        ProcedureOrderParams params = procedureQueryPort.findById(procedureId).orElse(null);
        if (params != null && "INTERVAL".equalsIgnoreCase(params.guidelineType())) {
            Integer interval = ProcedureScheduleGenerator.intervalHours(params.frequency());
            if (interval != null) recalcFollowing(all, idx, realTime, interval);
        }

        return all.stream().map(ProcedureScheduleDto::from).toList();
    }

    private static int indexOfId(List<ProcedureSchedule> all, Long id) {
        for (int i = 0; i < all.size(); i++) {
            if (id.equals(all.get(i).getId())) return i;
        }
        throw new IllegalArgumentException("Procedure schedule not found in plan: " + id);
    }

    private void recalcFollowing(List<ProcedureSchedule> all, int pivotIdx,
                                 LocalDateTime from, int intervalHours) {
        LocalDateTime cursor = from;
        for (int i = pivotIdx + 1; i < all.size(); i++) {
            ProcedureSchedule s = all.get(i);
            if (s.getAppliedStatus() != AppliedStatus.PENDING) continue;
            cursor = cursor.plusHours(intervalHours);
            s.reschedule(cursor);
            repository.save(s);
        }
    }
}
