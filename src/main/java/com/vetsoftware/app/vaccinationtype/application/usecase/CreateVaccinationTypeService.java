package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.command.CreateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.CreateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.CompanyRef;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "vaccination.type.create")
@Service
public class CreateVaccinationTypeService implements CreateVaccinationTypeUseCase {
    private final VaccinationTypeRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateVaccinationTypeService(VaccinationTypeRepository repository,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public VaccinationTypeDto execute(CreateVaccinationTypeCommand command) {
        CompanyRef company = command.companyId() == null
                ? null
                : companyQueryPort.findById(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Company not found: " + command.companyId()));
        return VaccinationTypeDto.from(repository.save(VaccinationType.create(command.name(),
                command.description(), company, command.general())));
    }
}
