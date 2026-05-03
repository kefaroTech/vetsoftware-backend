package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.command.UpdateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.UpdateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination_type.update")
@Service
public class UpdateVaccinationTypeService implements UpdateVaccinationTypeUseCase {
    private final VaccinationTypeRepository repository;

    public UpdateVaccinationTypeService(VaccinationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public VaccinationTypeDto execute(UpdateVaccinationTypeCommand command) {
        VaccinationType vaccinationType = repository.findById(command.id())
                .orElseThrow(() -> new VaccinationTypeNotFoundException(command.id()));
        vaccinationType.update(command.name(), command.description());
        return VaccinationTypeDto.from(repository.save(vaccinationType));
    }
}
