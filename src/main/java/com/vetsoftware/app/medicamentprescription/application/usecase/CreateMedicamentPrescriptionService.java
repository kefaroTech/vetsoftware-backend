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
            PrescriptionQueryPort prescriptionQueryPort, MedicamentQueryPort medicamentQueryPort) {
        this.repository = repository;
        this.prescriptionQueryPort = prescriptionQueryPort;
        this.medicamentQueryPort = medicamentQueryPort;
    }

    /**
     * Las dos referencias se resuelven acotadas por empresa, igual que hace
     * {@link UpdateMedicamentPrescriptionService}. Sin ese filtro, cualquiera con
     * la autoridad {@code medicamentPrescription.create} podia colgar un
     * medicamento de la receta de otro tenant adivinando el id: el caso de uso no
     * carga ninguna entidad propia que valide la empresa, la receta ajena resolvia,
     * y el registro quedaba escrito en la receta de otra empresa.
     *
     * <p>
     * {@code companyId == null} es el camino SYSTEM (el controller lo pone con
     * {@code currentCompanyIdOrNull()}), que si puede operar sin acotar.
     */
    @Override
    public MedicamentPrescriptionDto execute(CreateMedicamentPrescriptionCommand command) {
        PrescriptionRef prescription = (command.companyId() == null
                ? prescriptionQueryPort.findById(command.prescriptionId())
                : prescriptionQueryPort.findByIdAndCompanyId(command.prescriptionId(),
                        command.companyId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prescription not found: " + command.prescriptionId()));
        MedicamentRef medicamentRef = (command.companyId() == null
                ? medicamentQueryPort.findById(command.medicamentId())
                : medicamentQueryPort.findAvailableById(command.medicamentId(),
                        command.companyId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medicament not found: " + command.medicamentId()));

        MedicamentPrescription medicament = MedicamentPrescription.create(medicamentRef,
                command.presentation(), command.quantity(), command.posology(),
                command.observation(), prescription);
        return MedicamentPrescriptionDto.from(repository.save(medicament));
    }
}
