package com.vetsoftware.app.spa.application.usecase;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.in.ListSpasByAnimalUseCase;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "spa.list.byAnimal")
@Service
public class ListSpasByAnimalService implements ListSpasByAnimalUseCase {
    private final SpaRepository repository;

    public ListSpasByAnimalService(SpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SpaDto> listByAnimal(Long animalId) {
        return repository.findAllByAnimalId(animalId).stream().map(SpaDto::from).toList();
    }
}
