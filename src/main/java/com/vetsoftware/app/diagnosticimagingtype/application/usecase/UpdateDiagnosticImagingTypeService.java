package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.command.UpdateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.UpdateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic.imaging.type.update")
@Service
public class UpdateDiagnosticImagingTypeService implements UpdateDiagnosticImagingTypeUseCase {
  private final DiagnosticImagingTypeRepository repository;
  private final CompanyQueryPort companyQueryPort;

  public UpdateDiagnosticImagingTypeService(
      DiagnosticImagingTypeRepository repository, CompanyQueryPort companyQueryPort) {
    this.repository = repository;
    this.companyQueryPort = companyQueryPort;
  }

  @Override
  @Transactional
  public DiagnosticImagingTypeDto execute(UpdateDiagnosticImagingTypeCommand command) {
    DiagnosticImagingType type =
        repository
            .findById(command.id())
            .orElseThrow(() -> new DiagnosticImagingTypeNotFoundException(command.id()));
    CompanyRef company =
        command.companyId() == null
            ? null
            : companyQueryPort
                .findById(command.companyId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException("Company not found: " + command.companyId()));
    type.update(command.name(), command.description(), company, command.general());
    return DiagnosticImagingTypeDto.from(repository.save(type));
  }
}
