package com.vetsoftware.app.medicationschedule.application.usecase;

import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.in.SuspendPendingMedicationSchedulesUseCase;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medication.schedule.suspend.pending")
@Service
public class SuspendPendingMedicationSchedulesService
        implements
            SuspendPendingMedicationSchedulesUseCase {
    private final MedicationScheduleRepository repository;

    public SuspendPendingMedicationSchedulesService(MedicationScheduleRepository repository) {
        this.repository = repository;
    }

    /**
     * No hay lectura previa que valide la propiedad —se escribe primero y se
     * devuelve lo que quedó vivo—, así que el {@code AND company_id} del UPDATE es
     * la única barrera. Con una orden de otro tenant el UPDATE acotado no toca
     * ninguna fila y la lectura acotada devuelve vacío: ni se suspende nada ni se
     * filtra que la orden existe. {@code companyId == null} es el camino SYSTEM.
     */
    @Override
    @Transactional
    public List<MedicationScheduleDto> execute(Long hospitalizationMedicationId, Long companyId) {
        if (companyId == null) {
            repository.disablePendingByHospitalizationMedicationId(hospitalizationMedicationId);
        } else {
            repository.disablePendingByHospitalizationMedicationId(hospitalizationMedicationId,
                    companyId);
        }
        // Quedan solo las aplicadas (enabled=true).
        return (companyId == null
                ? repository.findByHospitalizationMedicationId(hospitalizationMedicationId)
                : repository.findByHospitalizationMedicationIdAndCompanyId(
                        hospitalizationMedicationId, companyId))
                .stream().map(MedicationScheduleDto::from).toList();
    }
}
