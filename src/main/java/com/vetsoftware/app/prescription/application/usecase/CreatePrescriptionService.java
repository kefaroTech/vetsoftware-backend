package com.vetsoftware.app.prescription.application.usecase;

import com.vetsoftware.app.prescription.application.command.CreatePrescriptionCommand;
import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.in.CreatePrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.prescription.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.prescription.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.AnimalRef;
import com.vetsoftware.app.prescription.domain.CompanyRef;
import com.vetsoftware.app.prescription.domain.ConsultationRef;
import com.vetsoftware.app.prescription.domain.Prescription;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "prescription.create")
@Service
public class CreatePrescriptionService implements CreatePrescriptionUseCase {
    private final PrescriptionRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public CreatePrescriptionService(PrescriptionRepository repository,
            AnimalQueryPort animalQueryPort, ConsultationQueryPort consultationQueryPort,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public PrescriptionDto execute(CreatePrescriptionCommand command) {
        // Las dos referencias se resuelven acotadas: sin el companyId, un animalId o
        // un consultationId de otro tenant colgaba una receta de esta empresa de la
        // historia clinica de la vecina. El companyId lo inyecta el controller desde
        // el principal (authz.currentCompanyId()), asi que nunca es null aqui.
        AnimalRef animal = animalQueryPort
                .findByIdAndCompanyId(command.animalId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        ConsultationRef consultation = consultationQueryPort
                .findByIdAndCompanyId(command.consultationId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Consultation not found: " + command.consultationId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        Prescription prescription = Prescription.create(command.date(), command.diagnosis(),
                command.observations(), animal, consultation, company);
        return PrescriptionDto.from(repository.save(prescription));
    }
}
