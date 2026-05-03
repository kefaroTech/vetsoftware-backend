package com.vetsoftware.app.animal.application.dto;

import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalColor;
import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.WeightType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnimalDto(
        Long id, String name, String code,
        SpecieSummaryDto specie, BreedSummaryDto breed, OwnerSummaryDto owner,
        Gender gender, WeightType weightType, AnimalType animalType,
        ReproductiveState reproductiveState, AnimalColor color, LocalDate bod,
        Integer weight, Integer size, boolean deceased, LocalDate deceasedDate,
        CompanySummaryDto company, LocalDateTime createdDate
) {
    public static AnimalDto from(Animal animal) {
        return new AnimalDto(
            animal.getId(), animal.getName(), animal.getCode(),
            SpecieSummaryDto.from(animal.getSpecie()),
            BreedSummaryDto.from(animal.getBreed()),
            OwnerSummaryDto.from(animal.getOwner()),
            animal.getGender(), animal.getWeightType(), animal.getAnimalType(),
            animal.getReproductiveState(), animal.getColor(), animal.getBod(),
            animal.getWeight(), animal.getSize(), animal.isDeceased(), animal.getDeceasedDate(),
            CompanySummaryDto.from(animal.getCompany()), animal.getCreatedDate()
        );
    }
}
