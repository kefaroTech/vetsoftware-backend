package com.vetsoftware.app.numberingresolution.application.usecase;

import com.vetsoftware.app.numberingresolution.application.command.UpdateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.in.UpdateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.out.BranchQueryPort;
import com.vetsoftware.app.numberingresolution.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.CompanyRef;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionAlreadyActiveException;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "numbering.resolution.update")
@Service
public class UpdateNumberingResolutionService implements UpdateNumberingResolutionUseCase {
  private final NumberingResolutionRepository repository;
  private final CompanyQueryPort companyQueryPort;
  private final BranchQueryPort branchQueryPort;

  public UpdateNumberingResolutionService(
      NumberingResolutionRepository repository,
      CompanyQueryPort companyQueryPort,
      BranchQueryPort branchQueryPort) {
    this.repository = repository;
    this.companyQueryPort = companyQueryPort;
    this.branchQueryPort = branchQueryPort;
  }

  @Override
  @Transactional
  public NumberingResolutionDto execute(UpdateNumberingResolutionCommand command) {
    NumberingResolution resolution =
        repository
            .findById(command.id())
            .orElseThrow(() -> new NumberingResolutionNotFoundException(command.id()));
    CompanyRef company =
        companyQueryPort
            .findById(command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
    // Multi-sucursal (B-6): si es una resolución de sede, la sede debe pertenecer a la empresa.
    if (command.branchId() != null
        && !branchQueryPort.existsByIdAndCompanyId(command.branchId(), command.companyId())) {
      throw new IllegalArgumentException("Branch not found: " + command.branchId());
    }
    // Invariante "una sola activa por (empresa, SEDE, tipo)": si el update mueve la resolución a un
    // alcance
    // distinto que ya tiene otra activa, conflicto. (Si el alcance no cambia, no se chequea para no
    // contarse
    // a sí misma; la BD respalda cualquier carrera vía uq_numbering_resolutions_active_scope.)
    boolean scopeChanged =
        !command.companyId().equals(resolution.getCompany().id())
            || !Objects.equals(command.branchId(), resolution.getBranchId())
            || command.documentType() != resolution.getDocumentType();
    if (scopeChanged
        && repository.existsActiveByCompanyBranchAndType(
            command.companyId(), command.branchId(), command.documentType())) {
      throw new NumberingResolutionAlreadyActiveException(
          command.companyId(), command.documentType());
    }
    resolution.update(
        company,
        command.documentType(),
        command.resolutionNumber(),
        command.resolutionDate(),
        command.prefix(),
        command.rangeFrom(),
        command.rangeTo(),
        command.validFrom(),
        command.validTo(),
        command.technicalKey(),
        command.branchId());
    return NumberingResolutionDto.from(repository.save(resolution));
  }
}
