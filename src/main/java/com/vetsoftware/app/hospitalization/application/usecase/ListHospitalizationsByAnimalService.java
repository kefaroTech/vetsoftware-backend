package com.vetsoftware.app.hospitalization.application.usecase;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.in.ListHospitalizationsByAnimalUseCase;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.list.by.animal")
@Service
public class ListHospitalizationsByAnimalService implements ListHospitalizationsByAnimalUseCase {
    private final HospitalizationRepository repository;

    public ListHospitalizationsByAnimalService(HospitalizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<HospitalizationDto> listByAnimal(Long animalId) {
        return repository.findAllByAnimalId(animalId).stream().map(HospitalizationDto::from).toList();
    }
}
