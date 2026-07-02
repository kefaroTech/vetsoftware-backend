package com.vetsoftware.app.consultation.application.usecase;

import com.vetsoftware.app.consultation.application.command.UpdateConsultationCommand;
import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.port.in.UpdateConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.consultation.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.application.port.out.ConsultationTypeQueryPort;
import com.vetsoftware.app.consultation.domain.AnimalRef;
import com.vetsoftware.app.consultation.domain.CompanyRef;
import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultation.domain.ConsultationNotFoundException;
import com.vetsoftware.app.consultation.domain.ConsultationTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "consultation.update")
@Service
public class UpdateConsultationService implements UpdateConsultationUseCase {
    private final ConsultationRepository repository;
    private final ConsultationTypeQueryPort consultationTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public UpdateConsultationService(ConsultationRepository repository,
                                     ConsultationTypeQueryPort consultationTypeQueryPort,
                                     AnimalQueryPort animalQueryPort,
                                     CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.consultationTypeQueryPort = consultationTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public ConsultationDto execute(UpdateConsultationCommand command) {
        Consultation consultation = repository.findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new ConsultationNotFoundException(command.id()));
        ConsultationTypeRef consultationType = consultationTypeQueryPort.findById(command.consultationTypeId())
            .orElseThrow(() -> new IllegalArgumentException("ConsultationType not found: " + command.consultationTypeId()));
        AnimalRef animal = animalQueryPort.findByIdAndCompanyId(command.animalId(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));

        consultation.update(command.date(), consultationType, command.anamnesis(), command.diagnosis(),
            command.therapeuticPlan(), command.diagnosisPlan(), command.nextControl(),
            animal, company);
        return ConsultationDto.from(repository.save(consultation));
    }
}
