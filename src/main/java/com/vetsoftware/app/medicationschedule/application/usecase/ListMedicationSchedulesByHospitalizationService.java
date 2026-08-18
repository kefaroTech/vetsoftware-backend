package com.vetsoftware.app.medicationschedule.application.usecase;

import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.in.ListMedicationSchedulesByHospitalizationUseCase;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "medication.schedule.list.by.hospitalization")
@Service
public class ListMedicationSchedulesByHospitalizationService
        implements
            ListMedicationSchedulesByHospitalizationUseCase {
    private final MedicationScheduleRepository repository;

    public ListMedicationSchedulesByHospitalizationService(
            MedicationScheduleRepository repository) {
        this.repository = repository;
    }

    /**
     * {@code companyId == null} es el camino SYSTEM, cross-tenant por diseño. Un
     * empleado solo ve la hoja de medicación de las hospitalizaciones de su
     * empresa: la hospitalización ajena devuelve la lista vacía, no un 403 que
     * confirme que existe.
     */
    @Override
    public List<MedicationScheduleDto> listByHospitalization(Long hospitalizationId,
            Long companyId) {
        return (companyId == null
                ? repository.findByHospitalizationId(hospitalizationId)
                : repository.findByHospitalizationIdAndCompanyId(hospitalizationId, companyId))
                .stream().map(MedicationScheduleDto::from).toList();
    }
}
