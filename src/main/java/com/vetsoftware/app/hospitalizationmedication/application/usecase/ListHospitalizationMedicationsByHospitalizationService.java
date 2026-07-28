package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.ListHospitalizationMedicationsByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.medication.list.by.hospitalization")
@Service
public class ListHospitalizationMedicationsByHospitalizationService
        implements ListHospitalizationMedicationsByHospitalizationUseCase {
    private final HospitalizationMedicationRepository repository;

    public ListHospitalizationMedicationsByHospitalizationService(HospitalizationMedicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<HospitalizationMedicationDto> listByHospitalization(Long hospitalizationId) {
        return repository.findAllByHospitalizationId(hospitalizationId).stream()
            .map(HospitalizationMedicationDto::from).toList();
    }
}
