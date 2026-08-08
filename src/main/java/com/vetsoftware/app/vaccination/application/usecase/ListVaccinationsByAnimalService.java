package com.vetsoftware.app.vaccination.application.usecase;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.dto.PageResult;
import com.vetsoftware.app.vaccination.application.port.in.ListVaccinationsByAnimalUseCase;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "vaccination.list.by.animal")
@Service
public class ListVaccinationsByAnimalService implements ListVaccinationsByAnimalUseCase {
    private final VaccinationRepository repository;

    public ListVaccinationsByAnimalService(VaccinationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<VaccinationDto> listByAnimal(Long animalId, String query, int page,
            int pageSize) {
        return repository.findAllByAnimalId(animalId, query, page, pageSize)
                .map(VaccinationDto::from);
    }
}
