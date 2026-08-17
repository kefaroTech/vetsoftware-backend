package com.vetsoftware.app.medicamentprescription.application.usecase;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.medicamentprescription.application.port.in.ListMedicamentPrescriptionsUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "medicament.prescription.list")
@Service
public class ListMedicamentPrescriptionsService implements ListMedicamentPrescriptionsUseCase {
    private final MedicamentPrescriptionRepository repository;

    public ListMedicamentPrescriptionsService(MedicamentPrescriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<MedicamentPrescriptionDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(MedicamentPrescriptionDto::from);
    }
}
