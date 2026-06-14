package com.vetsoftware.app.numberingresolution.application.usecase;

import com.vetsoftware.app.numberingresolution.application.command.UpdateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.in.UpdateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.CompanyRef;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "numberingResolution.update")
@Service
public class UpdateNumberingResolutionService implements UpdateNumberingResolutionUseCase {
    private final NumberingResolutionRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public UpdateNumberingResolutionService(NumberingResolutionRepository repository,
                                            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public NumberingResolutionDto execute(UpdateNumberingResolutionCommand command) {
        NumberingResolution resolution = repository.findById(command.id())
                .orElseThrow(() -> new NumberingResolutionNotFoundException(command.id()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        resolution.update(company, command.documentType(), command.resolutionNumber(), command.resolutionDate(),
                command.prefix(), command.rangeFrom(), command.rangeTo(),
                command.validFrom(), command.validTo(), command.technicalKey());
        return NumberingResolutionDto.from(repository.save(resolution));
    }
}
