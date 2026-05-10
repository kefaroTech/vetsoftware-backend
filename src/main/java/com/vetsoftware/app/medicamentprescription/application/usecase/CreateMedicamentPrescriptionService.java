package com.vetsoftware.app.medicamentprescription.application.usecase;

import com.vetsoftware.app.medicamentprescription.application.command.CreateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.in.CreateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.application.port.out.PrescriptionQueryPort;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "medicament_prescription.create")
@Service
public class CreateMedicamentPrescriptionService implements CreateMedicamentPrescriptionUseCase {
    private final MedicamentPrescriptionRepository repository;
    private final PrescriptionQueryPort prescriptionQueryPort;

    public CreateMedicamentPrescriptionService(MedicamentPrescriptionRepository repository,
                                               PrescriptionQueryPort prescriptionQueryPort) {
        this.repository = repository;
        this.prescriptionQueryPort = prescriptionQueryPort;
    }

    @Override
    public MedicamentPrescriptionDto execute(CreateMedicamentPrescriptionCommand command) {
        PrescriptionRef prescription = prescriptionQueryPort.findById(command.prescriptionId())
            .orElseThrow(() -> new IllegalArgumentException("Prescription not found: " + command.prescriptionId()));

        MedicamentPrescription medicament = MedicamentPrescription.create(
            command.name(), command.presentation(), command.quantity(),
            command.posology(), prescription);
        return MedicamentPrescriptionDto.from(repository.save(medicament));
    }
}
