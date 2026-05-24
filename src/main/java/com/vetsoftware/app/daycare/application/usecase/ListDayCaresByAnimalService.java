package com.vetsoftware.app.daycare.application.usecase;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.ListDayCaresByAnimalUseCase;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "dayCare.list.byAnimal")
@Service
public class ListDayCaresByAnimalService implements ListDayCaresByAnimalUseCase {
    private final DayCareRepository repository;

    public ListDayCaresByAnimalService(DayCareRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DayCareDto> listByAnimal(Long animalId) {
        return repository.findAllByAnimalId(animalId).stream().map(DayCareDto::from).toList();
    }
}
