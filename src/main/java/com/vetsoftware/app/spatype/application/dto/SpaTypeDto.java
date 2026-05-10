package com.vetsoftware.app.spatype.application.dto;

import com.vetsoftware.app.spatype.domain.SpaType;
import java.time.LocalDateTime;

public record SpaTypeDto(Long id, String name, String description, LocalDateTime createdDate) {
    public static SpaTypeDto from(SpaType spaType) {
        return new SpaTypeDto(
                spaType.getId(),
                spaType.getName(),
                spaType.getDescription(),
                spaType.getCreatedDate());
    }
}
