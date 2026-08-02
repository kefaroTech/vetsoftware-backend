package com.vetsoftware.app.surgery.application.usecase;

import com.vetsoftware.app.surgery.application.command.CreateSurgeryCommand;
import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.in.CreateSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.surgery.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.surgery.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.application.port.out.SurgeryTypeQueryPort;
import com.vetsoftware.app.surgery.domain.AnimalRef;
import com.vetsoftware.app.surgery.domain.CompanyRef;
import com.vetsoftware.app.surgery.domain.ConsultationRef;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "surgery.create")
@Service
public class CreateSurgeryService implements CreateSurgeryUseCase {
    private final SurgeryRepository repository;
    private final SurgeryTypeQueryPort surgeryTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public CreateSurgeryService(SurgeryRepository repository,
            SurgeryTypeQueryPort surgeryTypeQueryPort, AnimalQueryPort animalQueryPort,
            ConsultationQueryPort consultationQueryPort, CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.surgeryTypeQueryPort = surgeryTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public SurgeryDto execute(CreateSurgeryCommand command) {
        SurgeryTypeRef surgeryType = surgeryTypeQueryPort.findById(command.surgeryTypeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "SurgeryType not found: " + command.surgeryTypeId()));
        AnimalRef animal = animalQueryPort.findById(command.animalId()).orElseThrow(
                () -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ConsultationRef consultation = command.consultationId() == null
                ? null
                : consultationQueryPort.findById(command.consultationId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Consultation not found: " + command.consultationId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        Surgery surgery = Surgery.create(command.date(), surgeryType, command.description(),
                command.medicament(), command.observations(), command.complications(), animal,
                consultation, company);
        return SurgeryDto.from(repository.save(surgery));
    }
}
