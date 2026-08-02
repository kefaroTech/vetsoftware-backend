package com.vetsoftware.app.surgerytype.application.usecase;

import com.vetsoftware.app.surgerytype.application.command.CreateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.in.CreateSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.CompanyRef;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "surgery.type.create")
@Service
public class CreateSurgeryTypeService implements CreateSurgeryTypeUseCase {
  private final SurgeryTypeRepository repository;
  private final CompanyQueryPort companyQueryPort;

  public CreateSurgeryTypeService(
      SurgeryTypeRepository repository, CompanyQueryPort companyQueryPort) {
    this.repository = repository;
    this.companyQueryPort = companyQueryPort;
  }

  @Override
  public SurgeryTypeDto execute(CreateSurgeryTypeCommand command) {
    CompanyRef company =
        command.companyId() == null
            ? null
            : companyQueryPort
                .findById(command.companyId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException("Company not found: " + command.companyId()));
    return SurgeryTypeDto.from(
        repository.save(
            SurgeryType.create(command.name(), command.description(), company, command.general())));
  }
}
