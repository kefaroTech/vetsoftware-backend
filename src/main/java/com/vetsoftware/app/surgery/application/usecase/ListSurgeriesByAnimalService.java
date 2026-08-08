package com.vetsoftware.app.surgery.application.usecase;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.dto.PageResult;
import com.vetsoftware.app.surgery.application.port.in.ListSurgeriesByAnimalUseCase;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "surgery.list.by.animal")
@Service
public class ListSurgeriesByAnimalService implements ListSurgeriesByAnimalUseCase {
    private final SurgeryRepository repository;

    public ListSurgeriesByAnimalService(SurgeryRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SurgeryDto> listByAnimal(Long animalId, String query, int page,
            int pageSize) {
        return repository.findAllByAnimalId(animalId, query, page, pageSize).map(SurgeryDto::from);
    }
}
