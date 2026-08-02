package com.vetsoftware.app.medicamentprescription.application.usecase;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.in.FindMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "medicament.prescription.find")
@Service
public class FindMedicamentPrescriptionService implements FindMedicamentPrescriptionUseCase {
    private final MedicamentPrescriptionRepository repository;

    public FindMedicamentPrescriptionService(MedicamentPrescriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public MedicamentPrescriptionDto findById(Long id, Long companyId) {
        return MedicamentPrescriptionDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new MedicamentPrescriptionNotFoundException(id)));
    }
}
