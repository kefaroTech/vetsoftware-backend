package com.vetsoftware.app.numberingresolution.application.usecase;

import com.vetsoftware.app.numberingresolution.application.command.CreateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.in.CreateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.CompanyRef;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionAlreadyActiveException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "numberingResolution.create")
@Service
public class CreateNumberingResolutionService implements CreateNumberingResolutionUseCase {
    private final NumberingResolutionRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateNumberingResolutionService(NumberingResolutionRepository repository,
                                            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public NumberingResolutionDto execute(CreateNumberingResolutionCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        // Invariante: una sola resolución activa por (empresa, tipo). La carrera la respalda el índice único
        // de BD (uq_numbering_resolutions_active); este check da el error de negocio amigable.
        if (repository.existsActiveByCompanyAndType(command.companyId(), command.documentType())) {
            throw new NumberingResolutionAlreadyActiveException(command.companyId(), command.documentType());
        }
        return NumberingResolutionDto.from(
                repository.save(NumberingResolution.create(
                        company, command.documentType(), command.resolutionNumber(), command.resolutionDate(),
                        command.prefix(), command.rangeFrom(), command.rangeTo(),
                        command.validFrom(), command.validTo(), command.technicalKey())));
    }
}
