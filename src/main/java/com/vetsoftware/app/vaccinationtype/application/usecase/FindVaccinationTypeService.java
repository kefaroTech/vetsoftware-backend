package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.FindVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "vaccination.type.find")
@Service
public class FindVaccinationTypeService implements FindVaccinationTypeUseCase {
    private final VaccinationTypeRepository repository;

    public FindVaccinationTypeService(VaccinationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public VaccinationTypeDto findById(Long id, Long companyId) {
        return VaccinationTypeDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new VaccinationTypeNotFoundException(id)));
    }
}
