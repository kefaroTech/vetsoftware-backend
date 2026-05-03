package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.FindVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "vaccination_type.find")
@Service
public class FindVaccinationTypeService implements FindVaccinationTypeUseCase {
    private final VaccinationTypeRepository repository;

    public FindVaccinationTypeService(VaccinationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public VaccinationTypeDto findById(Long id) {
        return VaccinationTypeDto.from(repository.findById(id)
                .orElseThrow(() -> new VaccinationTypeNotFoundException(id)));
    }
}
