package com.vetsoftware.app.numberingresolution.application.usecase;

import com.vetsoftware.app.numberingresolution.application.command.CreateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.in.CreateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.out.BranchQueryPort;
import com.vetsoftware.app.numberingresolution.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.CompanyRef;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionAlreadyActiveException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "numbering.resolution.create")
@Service
public class CreateNumberingResolutionService implements CreateNumberingResolutionUseCase {
    private final NumberingResolutionRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final BranchQueryPort branchQueryPort;

    public CreateNumberingResolutionService(NumberingResolutionRepository repository,
            CompanyQueryPort companyQueryPort, BranchQueryPort branchQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
        this.branchQueryPort = branchQueryPort;
    }

    @Override
    public NumberingResolutionDto execute(CreateNumberingResolutionCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
        // Multi-sucursal (B-6): si es una resolución de sede, la sede debe pertenecer a
        // la empresa.
        if (command.branchId() != null && !branchQueryPort
                .existsByIdAndCompanyId(command.branchId(), command.companyId())) {
            throw new IllegalArgumentException("Branch not found: " + command.branchId());
        }
        // Invariante: una sola resolución activa por (empresa, SEDE, tipo). La carrera
        // la respalda el
        // índice
        // único de BD (uq_numbering_resolutions_active_scope); este check da el error
        // de negocio
        // amigable.
        if (repository.existsActiveByCompanyBranchAndType(command.companyId(), command.branchId(),
                command.documentType())) {
            throw new NumberingResolutionAlreadyActiveException(command.companyId(),
                    command.documentType());
        }
        return NumberingResolutionDto.from(repository.save(NumberingResolution.create(company,
                command.documentType(), command.resolutionNumber(), command.resolutionDate(),
                command.prefix(), command.rangeFrom(), command.rangeTo(), command.validFrom(),
                command.validTo(), command.technicalKey(), command.branchId())));
    }
}
