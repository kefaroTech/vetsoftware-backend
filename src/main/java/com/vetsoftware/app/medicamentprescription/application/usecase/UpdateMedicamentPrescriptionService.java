package com.vetsoftware.app.medicamentprescription.application.usecase;

import com.vetsoftware.app.medicamentprescription.application.command.UpdateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.in.UpdateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentQueryPort;
import com.vetsoftware.app.medicamentprescription.application.port.out.PrescriptionQueryPort;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentRef;
import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medicament.prescription.update")
@Service
public class UpdateMedicamentPrescriptionService implements UpdateMedicamentPrescriptionUseCase {
    private final MedicamentPrescriptionRepository repository;
    private final PrescriptionQueryPort prescriptionQueryPort;
    private final MedicamentQueryPort medicamentQueryPort;

    public UpdateMedicamentPrescriptionService(MedicamentPrescriptionRepository repository,
                                               PrescriptionQueryPort prescriptionQueryPort,
                                               MedicamentQueryPort medicamentQueryPort) {
        this.repository = repository;
        this.prescriptionQueryPort = prescriptionQueryPort;
        this.medicamentQueryPort = medicamentQueryPort;
    }

    @Override
    @Transactional
    public MedicamentPrescriptionDto execute(UpdateMedicamentPrescriptionCommand command) {
        MedicamentPrescription medicament = repository.findById(command.id())
            .orElseThrow(() -> new MedicamentPrescriptionNotFoundException(command.id()));
        PrescriptionRef prescription = prescriptionQueryPort.findById(command.prescriptionId())
            .orElseThrow(() -> new IllegalArgumentException("Prescription not found: " + command.prescriptionId()));
        MedicamentRef medicamentRef = medicamentQueryPort.findById(command.medicamentId())
            .orElseThrow(() -> new IllegalArgumentException("Medicament not found: " + command.medicamentId()));

        medicament.update(medicamentRef, command.presentation(), command.quantity(),
            command.posology(), command.observation(), prescription);
        return MedicamentPrescriptionDto.from(repository.save(medicament));
    }
}
