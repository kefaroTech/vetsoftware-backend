package com.vetsoftware.app.medicamentprescription.application.usecase;

import com.vetsoftware.app.medicamentprescription.application.command.CreateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.in.CreateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentQueryPort;
import com.vetsoftware.app.medicamentprescription.application.port.out.PrescriptionQueryPort;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentRef;
import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "medicament.prescription.create")
@Service
public class CreateMedicamentPrescriptionService implements CreateMedicamentPrescriptionUseCase {
    private final MedicamentPrescriptionRepository repository;
    private final PrescriptionQueryPort prescriptionQueryPort;
    private final MedicamentQueryPort medicamentQueryPort;

    public CreateMedicamentPrescriptionService(MedicamentPrescriptionRepository repository,
                                               PrescriptionQueryPort prescriptionQueryPort,
                                               MedicamentQueryPort medicamentQueryPort) {
        this.repository = repository;
        this.prescriptionQueryPort = prescriptionQueryPort;
        this.medicamentQueryPort = medicamentQueryPort;
    }

    @Override
    public MedicamentPrescriptionDto execute(CreateMedicamentPrescriptionCommand command) {
        PrescriptionRef prescription = prescriptionQueryPort.findById(command.prescriptionId())
            .orElseThrow(() -> new IllegalArgumentException("Prescription not found: " + command.prescriptionId()));
        MedicamentRef medicamentRef = medicamentQueryPort.findById(command.medicamentId())
            .orElseThrow(() -> new IllegalArgumentException("Medicament not found: " + command.medicamentId()));

        MedicamentPrescription medicament = MedicamentPrescription.create(
            medicamentRef, command.presentation(), command.quantity(),
            command.posology(), command.observation(), prescription);
        return MedicamentPrescriptionDto.from(repository.save(medicament));
    }
}
