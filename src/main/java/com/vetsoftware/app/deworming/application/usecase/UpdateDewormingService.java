package com.vetsoftware.app.deworming.application.usecase;

import com.vetsoftware.app.deworming.application.command.UpdateDewormingCommand;
import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.in.UpdateDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.deworming.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.deworming.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.AnimalRef;
import com.vetsoftware.app.deworming.domain.CompanyRef;
import com.vetsoftware.app.deworming.domain.ConsultationRef;
import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.domain.DewormingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "deworming.update")
@Service
public class UpdateDewormingService implements UpdateDewormingUseCase {
  private final DewormingRepository repository;
  private final AnimalQueryPort animalQueryPort;
  private final ConsultationQueryPort consultationQueryPort;
  private final CompanyQueryPort companyQueryPort;

  public UpdateDewormingService(
      DewormingRepository repository,
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
  public DewormingDto execute(UpdateDewormingCommand command) {
    Deworming deworming =
        repository
            .findById(command.id())
            .orElseThrow(() -> new DewormingNotFoundException(command.id()));
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

    deworming.update(
        command.date(),
        command.lastDeworming(),
        command.type(),
        command.product(),
        command.dosage(),
        command.nextControl(),
        command.observations(),
        animal,
        consultation,
        company);
    return DewormingDto.from(repository.save(deworming));
  }
}
