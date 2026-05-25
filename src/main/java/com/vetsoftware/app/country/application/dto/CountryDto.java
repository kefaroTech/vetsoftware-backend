package com.vetsoftware.app.country.application.dto;

import com.vetsoftware.app.country.domain.Country;
import java.time.LocalDateTime;

public record CountryDto(Long id, String name, LocalDateTime createdDate, boolean enabled) {
    public static CountryDto from(Country country) {
        return new CountryDto(country.getId(), country.getName(), country.getCreatedDate(), country.isEnabled());
    }
}
