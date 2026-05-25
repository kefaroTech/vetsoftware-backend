package com.vetsoftware.app.animalcolor.application.dto;

import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import java.time.LocalDateTime;

public record AnimalColorDto(Long id, String name, LocalDateTime createdDate, boolean enabled) {
    public static AnimalColorDto from(AnimalColor color) {
        return new AnimalColorDto(color.getId(), color.getName(), color.getCreatedDate(), color.isEnabled());
    }
}
