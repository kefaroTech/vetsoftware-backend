package com.vetsoftware.app.prescription.application.usecase;

import com.vetsoftware.app.prescription.application.command.UpdatePrescriptionCommand;
import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.in.UpdatePrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.prescription.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.prescription.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.AnimalRef;
import com.vetsoftware.app.prescription.domain.CompanyRef;
import com.vetsoftware.app.prescription.domain.ConsultationRef;
import com.vetsoftware.app.prescription.domain.Prescription;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "prescription.update")
@Service
public class UpdatePrescriptionService implements UpdatePrescriptionUseCase {
  private final PrescriptionRepository repository;
  private final AnimalQueryPort animalQueryPort;
  private final ConsultationQueryPort consultationQueryPort;
  private final CompanyQueryPort companyQueryPort;

  public UpdatePrescriptionService(
      PrescriptionRepository repository,
      AnimalQueryPort animalQueryPort,
      ConsultationQueryPort consultationQueryPort,
      CompanyQueryPort companyQueryPort) {
    this.repository = repository;
    this.animalQueryPort = animalQueryPort;
    this.consultationQueryPort = consultationQueryPort;
    this.companyQueryPort = companyQueryPort;
  }

  @Override
  @Transactional
  public PrescriptionDto execute(UpdatePrescriptionCommand command) {
    Prescription prescription =
        (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
            .orElseThrow(() -> new PrescriptionNotFoundException(command.id()));
    Long companyId =
        command.companyId() == null ? prescription.getCompany().id() : command.companyId();
    AnimalRef animal =
        animalQueryPort
            .findByIdAndCompanyId(command.animalId(), companyId)
            .orElseThrow(
                () -> new IllegalArgumentException("Animal not found: " + command.animalId()));
    ConsultationRef consultation =
        consultationQueryPort
            .findByIdAndCompanyId(command.consultationId(), companyId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Consultation not found: " + command.consultationId()));
    CompanyRef company =
        companyQueryPort
            .findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

    prescription.update(
        command.date(), command.diagnosis(), command.observations(), animal, consultation, company);
    return PrescriptionDto.from(repository.save(prescription));
  }
}
