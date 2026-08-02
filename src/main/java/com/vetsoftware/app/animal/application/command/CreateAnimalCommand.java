package com.vetsoftware.app.animal.application.command;

import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.WeightType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateAnimalCommand(String name, String code, Long specieId, Long breedId,
        Long ownerId, Gender gender, WeightType weightType, AnimalType animalType,
        ReproductiveState reproductiveState, Long colorId, LocalDate bod, BigDecimal weight,
        Integer size, boolean deceased, LocalDate deceasedDate, Long companyId) {
}
