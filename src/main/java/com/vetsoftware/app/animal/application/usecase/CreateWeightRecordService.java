package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.command.CreateWeightRecordCommand;
import com.vetsoftware.app.animal.application.dto.WeightRecordDto;
import com.vetsoftware.app.animal.application.port.in.CreateWeightRecordUseCase;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.application.port.out.WeightRecordRepository;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.domain.AnimalRef;
import com.vetsoftware.app.animal.domain.WeightRecord;
import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Observed(name = "weightRecord.create")
@Service
public class CreateWeightRecordService implements CreateWeightRecordUseCase {
    private final AnimalRepository animalRepository;
    private final WeightRecordRepository weightRecordRepository;

    public CreateWeightRecordService(AnimalRepository animalRepository,
                                     WeightRecordRepository weightRecordRepository) {
        this.animalRepository = animalRepository;
        this.weightRecordRepository = weightRecordRepository;
    }

    @Override
    public WeightRecordDto execute(CreateWeightRecordCommand command) {
        Animal animal = animalRepository.findByIdAndCompanyId(command.animalId(), command.companyId())
            .orElseThrow(() -> new AnimalNotFoundException(command.animalId()));
        WeightType unit = command.unit() != null ? command.unit() : animal.getWeightType();
        LocalDate measuredAt = command.measuredAt() != null ? command.measuredAt() : LocalDate.now();
        AnimalRef animalRef = new AnimalRef(animal.getId(), animal.getName(), animal.getCode());
        WeightRecord record = WeightRecord.create(
            animalRef, command.value(), unit, measuredAt,
            WeightSource.MANUAL, null, command.note(), animal.getCompany());
        return WeightRecordDto.from(weightRecordRepository.save(record));
    }
}
