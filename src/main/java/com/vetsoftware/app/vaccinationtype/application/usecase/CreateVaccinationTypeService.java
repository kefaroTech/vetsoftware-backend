package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.command.CreateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.CreateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "vaccination_type.create")
@Service
public class CreateVaccinationTypeService implements CreateVaccinationTypeUseCase {
    private final VaccinationTypeRepository repository;

    public CreateVaccinationTypeService(VaccinationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public VaccinationTypeDto execute(CreateVaccinationTypeCommand command) {
        return VaccinationTypeDto.from(
                repository.save(VaccinationType.create(command.name(), command.description())));
    }
}
