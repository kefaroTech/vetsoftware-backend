package com.vetsoftware.app.animal.application.dto;

import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.WeightType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnimalDto(Long id, String name, String code, SpecieSummaryDto specie,
        BreedSummaryDto breed, OwnerSummaryDto owner, Gender gender, WeightType weightType,
        AnimalType animalType, ReproductiveState reproductiveState, AnimalColorSummaryDto color,
        LocalDate bod, BigDecimal weight, LocalDate weightMeasuredAt, Integer size,
        boolean deceased, LocalDate deceasedDate, CompanySummaryDto company,
        LocalDateTime createdDate, boolean enabled) {
    public static AnimalDto from(Animal animal) {
        // weight es el peso ACTUAL derivado del último WeightRecord (nullable si el
        // animal aún no tiene
        // registros). weightType refleja la unidad de ese último registro; si no hay,
        // cae a la unidad
        // preferida del animal. La serie completa vive en GET
        // /animals/{id}/weight-records.
        WeightType displayUnit = animal.getCurrentWeightUnit() != null
                ? animal.getCurrentWeightUnit()
                : animal.getWeightType();
        return new AnimalDto(animal.getId(), animal.getName(), animal.getCode(),
                SpecieSummaryDto.from(animal.getSpecie()), BreedSummaryDto.from(animal.getBreed()),
                OwnerSummaryDto.from(animal.getOwner()), animal.getGender(), displayUnit,
                animal.getAnimalType(), animal.getReproductiveState(),
                AnimalColorSummaryDto.from(animal.getColor()), animal.getBod(),
                animal.getCurrentWeight(), animal.getCurrentWeightMeasuredAt(), animal.getSize(),
                animal.isDeceased(), animal.getDeceasedDate(),
                CompanySummaryDto.from(animal.getCompany()), animal.getCreatedDate(),
                animal.isEnabled());
    }
}
