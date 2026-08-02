package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.command.CreateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.CreateMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "medicament.create")
@Service
public class CreateMedicamentService implements CreateMedicamentUseCase {
  private final MedicamentRepository repository;
  private final CompanyQueryPort companyQueryPort;

  public CreateMedicamentService(
      MedicamentRepository repository, CompanyQueryPort companyQueryPort) {
    this.repository = repository;
    this.companyQueryPort = companyQueryPort;
  }

  @Override
  public MedicamentDto execute(CreateMedicamentCommand command) {
    CompanyRef company =
        command.companyId() == null
            ? null
            : companyQueryPort
                .findById(command.companyId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException("Company not found: " + command.companyId()));
    return MedicamentDto.from(
        repository.save(
            Medicament.create(command.name(), command.description(), company, command.general())));
  }
}
