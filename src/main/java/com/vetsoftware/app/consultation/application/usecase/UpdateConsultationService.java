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
import com.vetsoftware.app.consultation.domain.PhysicalExam;
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
            ConsultationTypeQueryPort consultationTypeQueryPort, AnimalQueryPort animalQueryPort,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.consultationTypeQueryPort = consultationTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public ConsultationDto execute(UpdateConsultationCommand command) {
        Consultation consultation = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new ConsultationNotFoundException(command.id()));
        Long companyId = command.companyId() == null
                ? consultation.getCompany().id()
                : command.companyId();
        ConsultationTypeRef consultationType = consultationTypeQueryPort
                .findById(command.consultationTypeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "ConsultationType not found: " + command.consultationTypeId()));
        AnimalRef animal = animalQueryPort.findByIdAndCompanyId(command.animalId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        CompanyRef company = companyQueryPort.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        PhysicalExam physicalExam = new PhysicalExam(command.temperature(), command.heartRate(),
                command.respiratoryRate(), command.mucousMembranes(), command.capillaryRefill(),
                command.hydration(), command.bodyConditionScore(), command.painScore(),
                command.attitude(), command.examFindings());
        consultation.update(command.date(), consultationType, command.anamnesis(),
                command.diagnosis(), command.prognosis(), physicalExam, command.nextControl(),
                animal, company);
        return ConsultationDto.from(repository.save(consultation));
    }
}
