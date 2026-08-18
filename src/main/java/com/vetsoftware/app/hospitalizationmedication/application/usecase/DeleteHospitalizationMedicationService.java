package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import com.vetsoftware.app.hospitalizationmedication.application.port.in.DeleteHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.medication.delete")
@Service
public class DeleteHospitalizationMedicationService
        implements
            DeleteHospitalizationMedicationUseCase {
    private final HospitalizationMedicationRepository repository;

    public DeleteHospitalizationMedicationService(HospitalizationMedicationRepository repository) {
        this.repository = repository;
    }

    /**
     * La existencia se comprueba acotada por empresa: una orden de otro tenant es
     * indistinguible de una inexistente y sale como 404, sin llegar al delete.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new HospitalizationMedicationNotFoundException(id));
        repository.delete(id);
    }
}
