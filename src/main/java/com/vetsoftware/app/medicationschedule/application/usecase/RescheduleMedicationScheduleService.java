package com.vetsoftware.app.medicationschedule.application.usecase;

import com.vetsoftware.app.medicationschedule.application.command.RescheduleMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.in.RescheduleMedicationScheduleUseCase;
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

@Observed(name = "medication.schedule.reschedule")
@Service
public class RescheduleMedicationScheduleService implements RescheduleMedicationScheduleUseCase {
    private final MedicationScheduleRepository repository;
    private final HospitalizationMedicationQueryPort medicationQueryPort;

    public RescheduleMedicationScheduleService(MedicationScheduleRepository repository,
            HospitalizationMedicationQueryPort medicationQueryPort) {
        this.repository = repository;
        this.medicationQueryPort = medicationQueryPort;
    }

    /**
     * La toma no tiene empresa propia, asi que la propiedad se comprueba subiendo a
     * la orden de medicacion y de ahi a la hospitalizacion, que si la tiene. El
     * chequeo va <em>antes</em> del primer {@code reschedule}/{@code save}: en modo
     * cascada esto no movia una fila sino toda la pauta pendiente de un paciente de
     * otro tenant.
     *
     * <p>
     * Una vez validada la orden, el resto del plan es suyo por construccion: todas
     * las tomas cuelgan de la misma orden. {@code companyId == null} es el camino
     * SYSTEM.
     */
    @Override
    @Transactional
    public List<MedicationScheduleDto> execute(RescheduleMedicationScheduleCommand command) {
        if (command.newDateTime() == null)
            throw new IllegalArgumentException("newDateTime is required");

        MedicationSchedule probe = repository.findById(command.scheduleId())
                .orElseThrow(() -> notFound(command.scheduleId()));
        Long medicationId = probe.getHospitalizationMedication().id();
        MedicationOrderParams owned = requireOwnedByCompany(medicationId, command.companyId(),
                command.scheduleId());

        // Orden previo al movimiento (define "las siguientes").
        List<MedicationSchedule> all = new ArrayList<>(command.companyId() == null
                ? repository.findByHospitalizationMedicationId(medicationId)
                : repository.findByHospitalizationMedicationIdAndCompanyId(medicationId,
                        command.companyId()));
        all.sort(Comparator.comparing(MedicationSchedule::getCurrentDateTime));
        int idx = indexOfId(all, command.scheduleId());

        MedicationSchedule target = all.get(idx);
        target.reschedule(command.newDateTime());
        repository.save(target);

        if ("cascade".equalsIgnoreCase(command.mode())) {
            MedicationOrderParams params = owned != null
                    ? owned
                    : medicationQueryPort.findById(medicationId).orElse(null);
            if (params != null && "INTERVAL".equalsIgnoreCase(params.guidelineType())) {
                Integer interval = MedicationScheduleGenerator.intervalHours(params.frequency());
                if (interval != null)
                    recalcFollowing(all, idx, command.newDateTime(), interval);
            }
        }

        return all.stream().map(MedicationScheduleDto::from).toList();
    }

    /**
     * Devuelve la orden ya resuelta para no repetir la consulta en el modo cascada.
     * {@code null} solo en el camino SYSTEM, donde no hay empresa que acotar y la
     * orden se resuelve mas tarde si hace falta. Mismo mensaje que el id
     * inexistente: a un tenant ajeno no se le confirma que la toma existe.
     */
    private MedicationOrderParams requireOwnedByCompany(Long medicationId, Long companyId,
            Long scheduleId) {
        if (companyId == null) {
            return null;
        }
        return medicationQueryPort.findByIdAndCompanyId(medicationId, companyId)
                .orElseThrow(() -> notFound(scheduleId));
    }

    private static IllegalArgumentException notFound(Long scheduleId) {
        return new IllegalArgumentException("Medication schedule not found: " + scheduleId);
    }

    private static int indexOfId(List<MedicationSchedule> all, Long id) {
        for (int i = 0; i < all.size(); i++) {
            if (id.equals(all.get(i).getId()))
                return i;
        }
        throw new IllegalArgumentException("Medication schedule not found in plan: " + id);
    }

    private void recalcFollowing(List<MedicationSchedule> all, int pivotIdx, LocalDateTime from,
            int intervalHours) {
        LocalDateTime cursor = from;
        for (int i = pivotIdx + 1; i < all.size(); i++) {
            MedicationSchedule s = all.get(i);
            if (s.getAppliedStatus() != AppliedStatus.PENDING)
                continue;
            cursor = cursor.plusHours(intervalHours);
            s.reschedule(cursor);
            repository.save(s);
        }
    }
}
