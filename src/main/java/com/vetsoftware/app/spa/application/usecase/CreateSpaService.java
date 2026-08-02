package com.vetsoftware.app.spa.application.usecase;

import com.vetsoftware.app.spa.application.command.CreateSpaCommand;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.in.CreateSpaUseCase;
import com.vetsoftware.app.spa.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.spa.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.application.port.out.SpaTypeQueryPort;
import com.vetsoftware.app.spa.domain.AnimalRef;
import com.vetsoftware.app.spa.domain.CompanyRef;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.domain.SpaTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "spa.create")
@Service
public class CreateSpaService implements CreateSpaUseCase {
  private final SpaRepository repository;
  private final SpaTypeQueryPort spaTypeQueryPort;
  private final AnimalQueryPort animalQueryPort;
  private final CompanyQueryPort companyQueryPort;

  public CreateSpaService(
      SpaRepository repository,
      SpaTypeQueryPort spaTypeQueryPort,
      AnimalQueryPort animalQueryPort,
      CompanyQueryPort companyQueryPort) {
    this.repository = repository;
    this.spaTypeQueryPort = spaTypeQueryPort;
    this.animalQueryPort = animalQueryPort;
    this.companyQueryPort = companyQueryPort;
  }

  @Override
  public SpaDto execute(CreateSpaCommand command) {
    SpaTypeRef spaType =
        spaTypeQueryPort
            .findById(command.spaTypeId())
            .orElseThrow(
                () -> new IllegalArgumentException("SpaType not found: " + command.spaTypeId()));
    AnimalRef animal =
        animalQueryPort
            .findById(command.animalId())
            .orElseThrow(
                () -> new IllegalArgumentException("Animal not found: " + command.animalId()));
    CompanyRef company =
        companyQueryPort
            .findById(command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

    Spa spa =
        Spa.create(
            command.date(),
            spaType,
            command.reason(),
            command.details(),
            command.observations(),
            animal,
            company);
    return SpaDto.from(repository.save(spa));
  }
}
