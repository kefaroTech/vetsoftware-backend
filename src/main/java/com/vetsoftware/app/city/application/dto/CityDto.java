package com.vetsoftware.app.city.application.dto;

import com.vetsoftware.app.city.domain.City;
import java.time.LocalDateTime;

public record CityDto(Long id, String name, StateSummaryDto state, String daneCode, LocalDateTime createdDate, boolean enabled) {
    public static CityDto from(City city) {
        return new CityDto(
            city.getId(),
            city.getName(),
            StateSummaryDto.from(city.getState()),
            city.getDaneCode(),
            city.getCreatedDate(),
            city.isEnabled()
        );
    }
}
