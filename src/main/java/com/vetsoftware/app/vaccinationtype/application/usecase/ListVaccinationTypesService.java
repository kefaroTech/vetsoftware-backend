package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.ListVaccinationTypesUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "vaccination.type.list")
@Service
public class ListVaccinationTypesService implements ListVaccinationTypesUseCase {
    private final VaccinationTypeRepository repository;

    public ListVaccinationTypesService(VaccinationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<VaccinationTypeDto> listAll() {
        return repository.findAll().stream().map(VaccinationTypeDto::from).toList();
    }
}
