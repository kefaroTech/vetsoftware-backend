package com.vetsoftware.app.vaccination.application.usecase;

import com.vetsoftware.app.vaccination.application.command.UpdateVaccinationCommand;
import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.port.in.UpdateVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationTypeQueryPort;
import com.vetsoftware.app.vaccination.domain.AnimalRef;
import com.vetsoftware.app.vaccination.domain.CompanyRef;
import com.vetsoftware.app.vaccination.domain.ConsultationRef;
import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccination.domain.VaccinationNotFoundException;
import com.vetsoftware.app.vaccination.domain.VaccinationTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination.update")
@Service
public class UpdateVaccinationService implements UpdateVaccinationUseCase {
  private final VaccinationRepository repository;
  private final VaccinationTypeQueryPort vaccinationTypeQueryPort;
  private final AnimalQueryPort animalQueryPort;
  private final ConsultationQueryPort consultationQueryPort;
  private final CompanyQueryPort companyQueryPort;

  public UpdateVaccinationService(
      VaccinationRepository repository,
      VaccinationTypeQueryPort vaccinationTypeQueryPort,
      AnimalQueryPort animalQueryPort,
      ConsultationQueryPort consultationQueryPort,
      CompanyQueryPort companyQueryPort) {
    this.repository = repository;
    this.vaccinationTypeQueryPort = vaccinationTypeQueryPort;
    this.animalQueryPort = animalQueryPort;
    this.consultationQueryPort = consultationQueryPort;
    this.companyQueryPort = companyQueryPort;
  }

  @Override
  @Transactional
  public VaccinationDto execute(UpdateVaccinationCommand command) {
    Vaccination vaccination =
        repository
            .findById(command.id())
            .orElseThrow(() -> new VaccinationNotFoundException(command.id()));
    VaccinationTypeRef vaccinationType =
        vaccinationTypeQueryPort
            .findById(command.vaccinationTypeId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "VaccinationType not found: " + command.vaccinationTypeId()));
    AnimalRef animal =
        animalQueryPort
            .findById(command.animalId())
            .orElseThrow(
                () -> new IllegalArgumentException("Animal not found: " + command.animalId()));
    ConsultationRef consultation =
        command.consultationId() == null
            ? null
            : consultationQueryPort
                .findById(command.consultationId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Consultation not found: " + command.consultationId()));
    CompanyRef company =
        companyQueryPort
            .findById(command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

    vaccination.update(
        command.date(),
        vaccinationType,
        command.lot(),
        command.notes(),
        command.route(),
        command.applicationSite(),
        command.nextVaccination(),
        animal,
        consultation,
        company);
    return VaccinationDto.from(repository.save(vaccination));
  }
}
