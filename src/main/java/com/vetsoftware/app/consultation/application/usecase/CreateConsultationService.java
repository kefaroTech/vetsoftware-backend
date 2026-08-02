package com.vetsoftware.app.consultation.application.usecase;

import com.vetsoftware.app.consultation.application.command.CreateConsultationCommand;
import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.port.in.CreateConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.consultation.application.port.out.AnimalWeightPort;
import com.vetsoftware.app.consultation.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.application.port.out.ConsultationTypeQueryPort;
import com.vetsoftware.app.consultation.domain.AnimalRef;
import com.vetsoftware.app.consultation.domain.CompanyRef;
import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultation.domain.ConsultationTypeRef;
import com.vetsoftware.app.consultation.domain.PhysicalExam;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "consultation.create")
@Service
public class CreateConsultationService implements CreateConsultationUseCase {
  private final ConsultationRepository repository;
  private final ConsultationTypeQueryPort consultationTypeQueryPort;
  private final AnimalQueryPort animalQueryPort;
  private final CompanyQueryPort companyQueryPort;
  private final AnimalWeightPort animalWeightPort;

  public CreateConsultationService(
      ConsultationRepository repository,
      ConsultationTypeQueryPort consultationTypeQueryPort,
      AnimalQueryPort animalQueryPort,
      CompanyQueryPort companyQueryPort,
      AnimalWeightPort animalWeightPort) {
    this.repository = repository;
    this.consultationTypeQueryPort = consultationTypeQueryPort;
    this.animalQueryPort = animalQueryPort;
    this.companyQueryPort = companyQueryPort;
    this.animalWeightPort = animalWeightPort;
  }

  @Override
  @Transactional
  public ConsultationDto execute(CreateConsultationCommand command) {
    ConsultationTypeRef consultationType =
        consultationTypeQueryPort
            .findById(command.consultationTypeId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "ConsultationType not found: " + command.consultationTypeId()));
    AnimalRef animal =
        animalQueryPort
            .findByIdAndCompanyId(command.animalId(), command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Animal not found: " + command.animalId()));
    CompanyRef company =
        companyQueryPort
            .findById(command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

    PhysicalExam physicalExam =
        new PhysicalExam(
            command.temperature(),
            command.heartRate(),
            command.respiratoryRate(),
            command.mucousMembranes(),
            command.capillaryRefill(),
            command.hydration(),
            command.bodyConditionScore(),
            command.painScore(),
            command.attitude(),
            command.examFindings());
    Consultation consultation =
        Consultation.create(
            command.date(),
            consultationType,
            command.anamnesis(),
            command.diagnosis(),
            command.prognosis(),
            physicalExam,
            command.nextControl(),
            animal,
            company);
    Consultation saved = repository.save(consultation);

    // Peso opcional capturado en la consulta → punto de la serie temporal del animal (misma
    // transacción: si el registro de peso falla, la consulta también hace rollback).
    if (command.weight() != null) {
      animalWeightPort.recordConsultationWeight(
          command.animalId(),
          command.companyId(),
          command.weight(),
          command.weightUnit(),
          command.date(),
          saved.getId());
    }
    return ConsultationDto.from(saved);
  }
}
