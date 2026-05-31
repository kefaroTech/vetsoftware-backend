package com.vetsoftware.app.medicationschedule.application.usecase;

import com.vetsoftware.app.medicationschedule.application.command.ApplyMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.in.ApplyMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.out.HospitalizationMedicationQueryPort;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import com.vetsoftware.app.medicationschedule.domain.AppliedStatus;
import com.vetsoftware.app.medicationschedule.domain.MedicationOrderParams;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import com.vetsoftware.app.medicationschedule.domain.MedicationScheduleGenerator;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medication_schedule.apply")
@Service
public class ApplyMedicationScheduleService implements ApplyMedicationScheduleUseCase {
    private final MedicationScheduleRepository repository;
    private final HospitalizationMedicationQueryPort medicationQueryPort;

    public ApplyMedicationScheduleService(MedicationScheduleRepository repository,
                                          HospitalizationMedicationQueryPort medicationQueryPort) {
        this.repository = repository;
        this.medicationQueryPort = medicationQueryPort;
    }

    @Override
    @Transactional
    public List<MedicationScheduleDto> execute(ApplyMedicationScheduleCommand command) {
        MedicationSchedule probe = repository.findById(command.scheduleId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Medication schedule not found: " + command.scheduleId()));
        Long medicationId = probe.getHospitalizationMedication().id();

        List<MedicationSchedule> all = new ArrayList<>(repository.findByHospitalizationMedicationId(medicationId));
        all.sort(Comparator.comparing(MedicationSchedule::getCurrentDateTime));
        int idx = indexOfId(all, command.scheduleId());

        LocalDateTime realTime = LocalDateTime.now();
        MedicationSchedule target = all.get(idx);
        target.apply(realTime);
        repository.save(target);

        // Pauta INTERVALO: recalcula las pendientes posteriores desde la hora real.
        MedicationOrderParams params = medicationQueryPort.findById(medicationId).orElse(null);
        if (params != null && "INTERVAL".equalsIgnoreCase(params.guidelineType())) {
            Integer interval = MedicationScheduleGenerator.intervalHours(params.frequency());
            if (interval != null) recalcFollowing(all, idx, realTime, interval);
        }

        return all.stream().map(MedicationScheduleDto::from).toList();
    }

    private static int indexOfId(List<MedicationSchedule> all, Long id) {
        for (int i = 0; i < all.size(); i++) {
            if (id.equals(all.get(i).getId())) return i;
        }
        throw new IllegalArgumentException("Medication schedule not found in plan: " + id);
    }

    private void recalcFollowing(List<MedicationSchedule> all, int pivotIdx,
                                 LocalDateTime from, int intervalHours) {
        LocalDateTime cursor = from;
        for (int i = pivotIdx + 1; i < all.size(); i++) {
            MedicationSchedule s = all.get(i);
            if (s.getAppliedStatus() != AppliedStatus.PENDING) continue;
            cursor = cursor.plusHours(intervalHours);
            s.reschedule(cursor);
            repository.save(s);
        }
    }
}
