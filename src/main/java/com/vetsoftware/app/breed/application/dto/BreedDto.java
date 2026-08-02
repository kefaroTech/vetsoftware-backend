package com.vetsoftware.app.breed.application.dto;

import com.vetsoftware.app.breed.domain.Breed;
import java.time.LocalDateTime;

public record BreedDto(
    Long id, String name, SpecieSummaryDto specie, LocalDateTime createdDate, boolean enabled) {
  public static BreedDto from(Breed breed) {
    return new BreedDto(
        breed.getId(),
        breed.getName(),
        SpecieSummaryDto.from(breed.getSpecie()),
        breed.getCreatedDate(),
        breed.isEnabled());
  }
}
