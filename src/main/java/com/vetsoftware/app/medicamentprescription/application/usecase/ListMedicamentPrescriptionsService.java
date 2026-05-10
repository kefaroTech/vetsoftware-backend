package com.vetsoftware.app.medicamentprescription.application.usecase;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.in.ListMedicamentPrescriptionsUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "medicament_prescription.list")
@Service
public class ListMedicamentPrescriptionsService implements ListMedicamentPrescriptionsUseCase {
    private final MedicamentPrescriptionRepository repository;

    public ListMedicamentPrescriptionsService(MedicamentPrescriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MedicamentPrescriptionDto> listAll() {
        return repository.findAll().stream().map(MedicamentPrescriptionDto::from).toList();
    }
}
