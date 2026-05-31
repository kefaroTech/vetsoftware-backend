package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.FindHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization_medication.find")
@Service
public class FindHospitalizationMedicationService implements FindHospitalizationMedicationUseCase {
    private final HospitalizationMedicationRepository repository;

    public FindHospitalizationMedicationService(HospitalizationMedicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public HospitalizationMedicationDto findById(Long id) {
        return HospitalizationMedicationDto.from(repository.findById(id)
            .orElseThrow(() -> new HospitalizationMedicationNotFoundException(id)));
    }
}
