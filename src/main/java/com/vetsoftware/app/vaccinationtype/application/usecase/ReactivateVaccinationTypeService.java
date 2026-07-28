package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.ReactivateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination.type.reactivate")
@Service
public class ReactivateVaccinationTypeService implements ReactivateVaccinationTypeUseCase {
    private final VaccinationTypeRepository repository;

    public ReactivateVaccinationTypeService(VaccinationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public VaccinationTypeDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new VaccinationTypeNotFoundException(id);
        return VaccinationTypeDto.from(repository.findById(id)
            .orElseThrow(() -> new VaccinationTypeNotFoundException(id)));
    }
}
