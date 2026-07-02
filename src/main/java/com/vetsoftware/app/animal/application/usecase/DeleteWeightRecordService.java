package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.port.in.DeleteWeightRecordUseCase;
import com.vetsoftware.app.animal.application.port.out.WeightRecordRepository;
import com.vetsoftware.app.animal.domain.WeightRecordNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "weightRecord.delete")
@Service
public class DeleteWeightRecordService implements DeleteWeightRecordUseCase {
    private final WeightRecordRepository repository;

    public DeleteWeightRecordService(WeightRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id, Long animalId, Long companyId) {
        repository.findByIdAndAnimalIdAndCompanyId(id, animalId, companyId)
            .orElseThrow(() -> new WeightRecordNotFoundException(id));
        repository.delete(id, companyId);
    }
}
