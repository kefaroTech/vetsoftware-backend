package com.vetsoftware.app.deworming.application.usecase;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.deworming.application.port.in.ListDewormingsByAnimalUseCase;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "deworming.list.by.animal")
@Service
public class ListDewormingsByAnimalService implements ListDewormingsByAnimalUseCase {
    private final DewormingRepository repository;

    public ListDewormingsByAnimalService(DewormingRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<DewormingDto> listByAnimal(Long animalId, Long companyId, String query,
            int page, int pageSize) {
        return repository.findAllByAnimalIdAndCompanyId(animalId, companyId, query, page, pageSize)
                .map(DewormingDto::from);
    }
}
