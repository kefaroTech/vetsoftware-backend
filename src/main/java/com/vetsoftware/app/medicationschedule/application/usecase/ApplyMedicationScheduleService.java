package com.vetsoftware.app.medicationschedule.application.usecase;

import com.vetsoftware.app.medicationschedule.application.command.ApplyMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.in.ApplyMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.out.HospitalizationMedicationQueryPort;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medication.schedule.apply")
@Service
public class ApplyMedicationScheduleService implements ApplyMedicationScheduleUseCase {
    private final MedicationScheduleRepository repository;
    private final HospitalizationMedicationQueryPort medicationQueryPort;

    public ApplyMedicationScheduleService(MedicationScheduleRepository repository,
            HospitalizationMedicationQueryPort medicationQueryPort) {
        this.repository = repository;
        this.medicationQueryPort = medicationQueryPort;
    }

    /**
     * La toma no tiene empresa propia, asi que la propiedad se comprueba subiendo a
     * la orden de medicacion y de ahi a la hospitalizacion, que si la tiene. Sin
     * esa comprobacion, cualquiera con {@code hospitalization.update} marcaba como
     * APLICADA la toma de un paciente de otro tenant adivinando el id, y la hoja de
     * medicacion ajena quedaba falseada.
     *
     * <p>
     * El chequeo va <em>antes</em> de {@code apply} y de {@code save}: si falla, no
     * se ha escrito nada. {@code companyId == null} es el camino SYSTEM.
     */
    @Override
    @Transactional
    public List<MedicationScheduleDto> execute(ApplyMedicationScheduleCommand command) {
        MedicationSchedule target = repository.findById(command.scheduleId())
                .orElseThrow(() -> notFound(command.scheduleId()));
        Long medicationId = target.getHospitalizationMedication().id();
        requireOwnedByCompany(medicationId, command.companyId(), command.scheduleId());

        target.apply(LocalDateTime.now());
        repository.save(target);

        // Pauta INTERVALO: aplicar tarde NO recalcula las siguientes; eso solo ocurre
        // al
        // reprogramar una toma (drag&drop → reschedule mode=cascade).
        return (command.companyId() == null
                ? repository.findByHospitalizationMedicationId(medicationId)
                : repository.findByHospitalizationMedicationIdAndCompanyId(medicationId,
                        command.companyId()))
                .stream().map(MedicationScheduleDto::from).toList();
    }

    /**
     * Mismo mensaje que el id inexistente: a un tenant ajeno no se le confirma que
     * la toma existe.
     */
    private void requireOwnedByCompany(Long medicationId, Long companyId, Long scheduleId) {
        if (companyId == null) {
            return;
        }
        medicationQueryPort.findByIdAndCompanyId(medicationId, companyId)
                .orElseThrow(() -> notFound(scheduleId));
    }

    private static IllegalArgumentException notFound(Long scheduleId) {
        return new IllegalArgumentException("Medication schedule not found: " + scheduleId);
    }
}
