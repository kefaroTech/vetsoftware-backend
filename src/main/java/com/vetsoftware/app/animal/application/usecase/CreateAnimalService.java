package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.command.CreateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.in.CreateAnimalUseCase;
import com.vetsoftware.app.animal.application.port.out.AnimalColorQueryPort;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.application.port.out.BreedQueryPort;
import com.vetsoftware.app.animal.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.animal.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.animal.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animal.application.port.out.WeightRecordRepository;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalColorRef;
import com.vetsoftware.app.animal.domain.AnimalRef;
import com.vetsoftware.app.animal.domain.BreedRef;
import com.vetsoftware.app.animal.domain.CompanyRef;
import com.vetsoftware.app.animal.domain.OwnerRef;
import com.vetsoftware.app.animal.domain.SpecieRef;
import com.vetsoftware.app.animal.domain.WeightRecord;
import com.vetsoftware.app.animal.domain.WeightSource;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animal.create")
@Service
public class CreateAnimalService implements CreateAnimalUseCase {
    private final AnimalRepository repository;
    private final SpecieQueryPort specieQueryPort;
    private final BreedQueryPort breedQueryPort;
    private final OwnerQueryPort ownerQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final AnimalColorQueryPort animalColorQueryPort;
    private final WeightRecordRepository weightRecordRepository;

    public CreateAnimalService(AnimalRepository repository,
                               SpecieQueryPort specieQueryPort,
                               BreedQueryPort breedQueryPort,
                               OwnerQueryPort ownerQueryPort,
                               CompanyQueryPort companyQueryPort,
                               AnimalColorQueryPort animalColorQueryPort,
                               WeightRecordRepository weightRecordRepository) {
        this.repository = repository;
        this.specieQueryPort = specieQueryPort;
        this.breedQueryPort = breedQueryPort;
        this.ownerQueryPort = ownerQueryPort;
        this.companyQueryPort = companyQueryPort;
        this.animalColorQueryPort = animalColorQueryPort;
        this.weightRecordRepository = weightRecordRepository;
    }

    @Override
    @Transactional
    public AnimalDto execute(CreateAnimalCommand command) {
        SpecieRef specie = specieQueryPort.findById(command.specieId())
            .orElseThrow(() -> new IllegalArgumentException("Specie not found: " + command.specieId()));
        BreedRef breed = breedQueryPort.findById(command.breedId())
            .orElseThrow(() -> new IllegalArgumentException("Breed not found: " + command.breedId()));
        OwnerRef owner = ownerQueryPort.findByIdAndCompanyId(command.ownerId(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + command.ownerId()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        AnimalColorRef color = animalColorQueryPort.findById(command.colorId())
            .orElseThrow(() -> new IllegalArgumentException("AnimalColor not found: " + command.colorId()));

        Animal animal = Animal.create(
            command.name(), command.code(), specie, breed, owner,
            command.gender(), command.weightType(), command.animalType(),
            command.reproductiveState(), color, command.bod(),
            command.size(), command.deceased(), command.deceasedDate(),
            company
        );
        Animal saved = repository.save(animal);

        // Peso inicial opcional → primer punto de la serie temporal (source=MANUAL). El peso actual
        // del animal se deriva de este registro; no se guarda como escalar. Ver WeightRecord.
        if (command.weight() != null) {
            LocalDate measuredAt = LocalDate.now();
            AnimalRef animalRef = new AnimalRef(saved.getId(), saved.getName(), saved.getCode());
            weightRecordRepository.save(WeightRecord.create(
                animalRef, command.weight(), command.weightType(), measuredAt,
                WeightSource.MANUAL, null, null, saved.getCompany()));
            saved.applyCurrentWeight(command.weight(), command.weightType(), measuredAt);
        }
        return AnimalDto.from(saved);
    }
}
