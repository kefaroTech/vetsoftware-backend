package com.vetsoftware.app.diagnosticimaging.application.usecase;

import com.vetsoftware.app.diagnosticimaging.application.command.CreateDiagnosticImagingCommand;
import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.in.CreateDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingTypeQueryPort;
import com.vetsoftware.app.diagnosticimaging.domain.AnimalRef;
import com.vetsoftware.app.diagnosticimaging.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimaging.domain.ConsultationRef;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "diagnostic.imaging.create")
@Service
public class CreateDiagnosticImagingService implements CreateDiagnosticImagingUseCase {
    private final DiagnosticImagingRepository repository;
    private final DiagnosticImagingTypeQueryPort diagnosticImagingTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public CreateDiagnosticImagingService(DiagnosticImagingRepository repository,
            DiagnosticImagingTypeQueryPort diagnosticImagingTypeQueryPort,
            AnimalQueryPort animalQueryPort, ConsultationQueryPort consultationQueryPort,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.diagnosticImagingTypeQueryPort = diagnosticImagingTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public DiagnosticImagingDto execute(CreateDiagnosticImagingCommand command) {
        // Mismos puertos acotados que el update: nacer apuntando al animal de otro
        // tenant es la misma fuga que reapuntarse a el despues.
        DiagnosticImagingTypeRef type = diagnosticImagingTypeQueryPort
                .findAvailableByIdAndCompanyId(command.diagnosticImagingTypeId(),
                        command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "DiagnosticImagingType not found: " + command.diagnosticImagingTypeId()));
        AnimalRef animal = animalQueryPort
                .findByIdAndCompanyId(command.animalId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        ConsultationRef consultation = command.consultationId() == null
                ? null
                : consultationQueryPort
                        .findByIdAndCompanyId(command.consultationId(), command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Consultation not found: " + command.consultationId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        DiagnosticImaging imaging = DiagnosticImaging.create(command.date(), type,
                command.clinicalSigns(), command.studyType(), command.diagnosis(),
                command.observations(), animal, consultation, company);
        return DiagnosticImagingDto.from(repository.save(imaging));
    }
}
