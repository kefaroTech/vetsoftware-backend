package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.dto.WeightRecordDto;
import com.vetsoftware.app.animal.application.port.in.ListWeightRecordsByAnimalUseCase;
import com.vetsoftware.app.animal.application.port.out.WeightRecordRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "weightRecord.list.byAnimal")
@Service
public class ListWeightRecordsByAnimalService implements ListWeightRecordsByAnimalUseCase {
    private final WeightRecordRepository repository;

    public ListWeightRecordsByAnimalService(WeightRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<WeightRecordDto> listByAnimal(Long animalId, Long companyId) {
        return repository.findByAnimalIdAndCompanyId(animalId, companyId)
            .stream().map(WeightRecordDto::from).toList();
    }
}
