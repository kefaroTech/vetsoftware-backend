package com.vetsoftware.app.specie.application.dto;

import com.vetsoftware.app.specie.domain.Specie;
import java.time.LocalDateTime;

public record SpecieDto(Long id, String name, LocalDateTime createdDate, boolean enabled) {
    public static SpecieDto from(Specie specie) {
        return new SpecieDto(specie.getId(), specie.getName(), specie.getCreatedDate(), specie.isEnabled());
    }
}
