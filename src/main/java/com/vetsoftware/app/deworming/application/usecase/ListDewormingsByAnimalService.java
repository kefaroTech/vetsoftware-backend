package com.vetsoftware.app.deworming.application.usecase;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.dto.PageResult;
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
    public PageResult<DewormingDto> listByAnimal(Long animalId, String query, int page,
            int pageSize) {
        return repository.findAllByAnimalId(animalId, query, page, pageSize)
                .map(DewormingDto::from);
    }
}
