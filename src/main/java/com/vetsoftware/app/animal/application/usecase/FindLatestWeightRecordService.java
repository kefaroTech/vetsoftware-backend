package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.dto.WeightRecordDto;
import com.vetsoftware.app.animal.application.port.in.FindLatestWeightRecordUseCase;
import com.vetsoftware.app.animal.application.port.out.WeightRecordRepository;
import com.vetsoftware.app.animal.domain.WeightRecordNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "weightRecord.findLatest")
@Service
public class FindLatestWeightRecordService implements FindLatestWeightRecordUseCase {
    private final WeightRecordRepository repository;

    public FindLatestWeightRecordService(WeightRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public WeightRecordDto findLatest(Long animalId, Long companyId) {
        return repository.findLatestByAnimalIdAndCompanyId(animalId, companyId)
            .map(WeightRecordDto::from)
            .orElseThrow(() -> new WeightRecordNotFoundException(animalId));
    }
}
