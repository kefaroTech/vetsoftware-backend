package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import com.vetsoftware.app.hospitalizationmedication.application.port.in.DeleteHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization_medication.delete")
@Service
public class DeleteHospitalizationMedicationService implements DeleteHospitalizationMedicationUseCase {
    private final HospitalizationMedicationRepository repository;

    public DeleteHospitalizationMedicationService(HospitalizationMedicationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new HospitalizationMedicationNotFoundException(id));
        repository.delete(id);
    }
}
