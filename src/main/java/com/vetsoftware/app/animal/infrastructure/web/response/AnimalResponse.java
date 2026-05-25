package com.vetsoftware.app.animal.infrastructure.web.response;

import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.WeightType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnimalResponse(
        Long id, String name, String code,
        SpecieSummary specie, BreedSummary breed, OwnerSummary owner,
        Gender gender, WeightType weightType, AnimalType animalType,
        ReproductiveState reproductiveState, AnimalColorSummary color, LocalDate bod,
        Integer weight, Integer size, boolean deceased, LocalDate deceasedDate,
        CompanySummary company, LocalDateTime createdDate, boolean enabled
) {}
