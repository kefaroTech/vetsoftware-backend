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

    @Override
    public List<MedicationScheduleDto> listByHospitalization(Long hospitalizationId) {
        return repository.findByHospitalizationId(hospitalizationId).stream()
                .map(MedicationScheduleDto::from).toList();
    }
}
