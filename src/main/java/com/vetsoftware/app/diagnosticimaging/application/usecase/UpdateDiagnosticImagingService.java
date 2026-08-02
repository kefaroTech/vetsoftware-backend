package com.vetsoftware.app.diagnosticimaging.application.usecase;

import com.vetsoftware.app.diagnosticimaging.application.command.UpdateDiagnosticImagingCommand;
import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.in.UpdateDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingTypeQueryPort;
import com.vetsoftware.app.diagnosticimaging.domain.AnimalRef;
import com.vetsoftware.app.diagnosticimaging.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimaging.domain.ConsultationRef;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic.imaging.update")
@Service
public class UpdateDiagnosticImagingService implements UpdateDiagnosticImagingUseCase {
    private final DiagnosticImagingRepository repository;
    private final DiagnosticImagingTypeQueryPort diagnosticImagingTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public UpdateDiagnosticImagingService(DiagnosticImagingRepository repository,
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
    @Transactional
    public DiagnosticImagingDto execute(UpdateDiagnosticImagingCommand command) {
        DiagnosticImaging imaging = repository.findById(command.id())
                .orElseThrow(() -> new DiagnosticImagingNotFoundException(command.id()));
        DiagnosticImagingTypeRef type = diagnosticImagingTypeQueryPort
                .findById(command.diagnosticImagingTypeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "DiagnosticImagingType not found: " + command.diagnosticImagingTypeId()));
        AnimalRef animal = animalQueryPort.findById(command.animalId()).orElseThrow(
                () -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ConsultationRef consultation = command.consultationId() == null
                ? null
                : consultationQueryPort.findById(command.consultationId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Consultation not found: " + command.consultationId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        imaging.update(command.date(), type, command.clinicalSigns(), command.studyType(),
                command.diagnosis(), command.observations(), animal, consultation, company);
        return DiagnosticImagingDto.from(repository.save(imaging));
    }
}
