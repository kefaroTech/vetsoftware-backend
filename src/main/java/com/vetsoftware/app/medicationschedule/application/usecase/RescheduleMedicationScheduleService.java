package com.vetsoftware.app.medicationschedule.application.usecase;

import com.vetsoftware.app.medicationschedule.application.command.RescheduleMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.dto.RescheduleResultDto;
import com.vetsoftware.app.medicationschedule.application.port.in.RescheduleMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.out.HospitalizationMedicationQueryPort;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import com.vetsoftware.app.medicationschedule.domain.AppliedStatus;
import com.vetsoftware.app.medicationschedule.domain.CascadeSkipReason;
import com.vetsoftware.app.medicationschedule.domain.MedicationOrderParams;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import com.vetsoftware.app.medicationschedule.domain.MedicationScheduleGenerator;
import com.vetsoftware.app.medicationschedule.domain.RescheduleMode;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medication.schedule.reschedule")
@Service
public class RescheduleMedicationScheduleService implements RescheduleMedicationScheduleUseCase {
    private static final Logger log = LoggerFactory
            .getLogger(RescheduleMedicationScheduleService.class);

    /**
     * Orden del plan por hora vigente. Va con {@code nullsLast} porque ahora se usa
     * tambien <em>despues</em> de guardar: alli un {@code currentDateTime} nulo
     * reventaria con el trabajo ya hecho en la BD y la respuesta perdida. Sobre
     * valores no nulos ordena exactamente igual que antes.
     */
    private static final Comparator<MedicationSchedule> BY_CURRENT_DATE_TIME = Comparator.comparing(
            MedicationSchedule::getCurrentDateTime,
            Comparator.nullsLast(Comparator.naturalOrder()));

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
    public RescheduleResultDto execute(RescheduleMedicationScheduleCommand command) {
        if (command.newDateTime() == null)
            throw new IllegalArgumentException("newDateTime is required");
        if (command.mode() == null)
            throw new IllegalArgumentException("mode is required");

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
        all.sort(BY_CURRENT_DATE_TIME);
        int idx = indexOfId(all, command.scheduleId());

        MedicationSchedule target = all.get(idx);
        target.reschedule(command.newDateTime());
        repository.save(target);

        boolean cascadeRequested = command.mode() == RescheduleMode.CASCADE;
        CascadeSkipReason skipped = cascadeRequested
                ? applyCascade(all, idx, command, medicationId, owned)
                : null;

        // Reordenar antes de mapear: el pivote —y, con cascada, las siguientes— ya
        // no estan donde estaban, y devolver el orden de lectura hacia que el plan
        // saliera desordenado por la propia operacion que acaba de moverlo.
        all.sort(BY_CURRENT_DATE_TIME);
        List<MedicationScheduleDto> schedules = all.stream().map(MedicationScheduleDto::from)
                .toList();

        if (!cascadeRequested)
            return RescheduleResultDto.notCascaded(schedules);
        return skipped == null
                ? RescheduleResultDto.applied(schedules)
                : RescheduleResultDto.skipped(schedules, skipped);
    }

    /**
     * Aplica la cascada; devuelve {@code null} si se aplico, o el motivo por el que
     * no. Las tres salidas tienen que nombrarse: saltarlas en silencio deja un 200
     * con el plan intacto salvo el pivote, indistinguible de una cascada que si
     * corrio.
     */
    private CascadeSkipReason applyCascade(List<MedicationSchedule> all, int pivotIdx,
            RescheduleMedicationScheduleCommand command, Long medicationId,
            MedicationOrderParams owned) {
        MedicationOrderParams params = owned != null
                ? owned
                : medicationQueryPort.findById(medicationId).orElse(null);
        if (params == null)
            return skip(command, medicationId, CascadeSkipReason.MEDICATION_ORDER_NOT_FOUND);
        if (!"INTERVAL".equalsIgnoreCase(params.guidelineType()))
            return skip(command, medicationId, CascadeSkipReason.GUIDELINE_NOT_INTERVAL);
        Integer interval = MedicationScheduleGenerator.intervalHours(params.frequency());
        if (interval == null)
            return skip(command, medicationId, CascadeSkipReason.FREQUENCY_NOT_DISCRETE);
        recalcFollowing(all, pivotIdx, command.newDateTime(), interval);
        return null;
    }

    /**
     * Solo ids y un enum: nada de esto identifica a un paciente ni a una persona, y
     * la cardinalidad del motivo esta acotada a tres valores.
     */
    private static CascadeSkipReason skip(RescheduleMedicationScheduleCommand command,
            Long medicationId, CascadeSkipReason reason) {
        log.warn("Cascada de reprogramacion no aplicada: scheduleId={} medicationId={} reason={}",
                command.scheduleId(), medicationId, reason);
        return reason;
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
