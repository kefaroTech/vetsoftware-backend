package com.vetsoftware.app.surgery.application.usecase;

import com.vetsoftware.app.surgery.application.command.UpdateSurgeryCommand;
import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.in.UpdateSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.surgery.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.surgery.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.application.port.out.SurgeryTypeQueryPort;
import com.vetsoftware.app.surgery.domain.AnimalRef;
import com.vetsoftware.app.surgery.domain.CompanyRef;
import com.vetsoftware.app.surgery.domain.ConsultationRef;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgery.update")
@Service
public class UpdateSurgeryService implements UpdateSurgeryUseCase {
    private final SurgeryRepository repository;
    private final SurgeryTypeQueryPort surgeryTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public UpdateSurgeryService(SurgeryRepository repository,
            SurgeryTypeQueryPort surgeryTypeQueryPort, AnimalQueryPort animalQueryPort,
            ConsultationQueryPort consultationQueryPort, CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.surgeryTypeQueryPort = surgeryTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public SurgeryDto execute(UpdateSurgeryCommand command) {
        Surgery surgery = repository.findById(command.id())
                .orElseThrow(() -> new SurgeryNotFoundException(command.id()));
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

        surgery.update(command.date(), surgeryType, command.description(), command.medicament(),
                command.observations(), command.complications(), animal, consultation, company);
        return SurgeryDto.from(repository.save(surgery));
    }
}
