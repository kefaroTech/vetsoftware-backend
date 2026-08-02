package com.vetsoftware.app.animalalert.application.usecase;

import com.vetsoftware.app.animalalert.application.dto.AnimalAlertDto;
import com.vetsoftware.app.animalalert.application.port.in.ListAnimalAlertsByAnimalUseCase;
import com.vetsoftware.app.animalalert.application.port.out.AnimalAlertRepository;
import com.vetsoftware.app.animalalert.application.query.ListAnimalAlertsByAnimalQuery;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "animal.alert.list.by.animal")
@Service
public class ListAnimalAlertsByAnimalService implements ListAnimalAlertsByAnimalUseCase {
    private final AnimalAlertRepository repository;

    public ListAnimalAlertsByAnimalService(AnimalAlertRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AnimalAlertDto> execute(ListAnimalAlertsByAnimalQuery query) {
        return repository.findByAnimalIdAndCompanyId(query.animalId(), query.companyId()).stream()
                .map(AnimalAlertDto::from).toList();
    }
}
