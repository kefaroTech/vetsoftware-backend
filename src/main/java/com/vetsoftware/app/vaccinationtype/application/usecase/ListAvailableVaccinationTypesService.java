package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.ListAvailableVaccinationTypesUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "vaccination_type.list_available")
@Service
public class ListAvailableVaccinationTypesService implements ListAvailableVaccinationTypesUseCase {
    private final VaccinationTypeRepository repository;

    public ListAvailableVaccinationTypesService(VaccinationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<VaccinationTypeDto> listAvailable(Long companyId) {
        return repository.findAllAvailableForCompany(companyId).stream().map(VaccinationTypeDto::from).toList();
    }
}
