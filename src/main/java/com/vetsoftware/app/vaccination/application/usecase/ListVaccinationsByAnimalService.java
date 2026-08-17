package com.vetsoftware.app.vaccination.application.usecase;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.shared.pagination.PageResult;
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
    public PageResult<VaccinationDto> listByAnimal(Long animalId, Long companyId, String query,
            int page, int pageSize) {
        return repository.findAllByAnimalIdAndCompanyId(animalId, companyId, query, page, pageSize)
                .map(VaccinationDto::from);
    }
}
