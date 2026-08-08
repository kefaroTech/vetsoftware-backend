package com.vetsoftware.app.animalcolor.application.dto;

import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import java.time.LocalDateTime;

public record AnimalColorDto(Long id, String name, SpecieSummaryDto specie,
        LocalDateTime createdDate, boolean enabled) {
    public static AnimalColorDto from(AnimalColor color) {
        return new AnimalColorDto(color.getId(), color.getName(),
                SpecieSummaryDto.from(color.getSpecie()), color.getCreatedDate(),
                color.isEnabled());
    }
}
