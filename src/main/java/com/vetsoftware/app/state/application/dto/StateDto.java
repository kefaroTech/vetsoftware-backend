package com.vetsoftware.app.state.application.dto;

import com.vetsoftware.app.state.domain.State;
import java.time.LocalDateTime;

public record StateDto(Long id, String name, CountrySummaryDto country, LocalDateTime createdDate) {
    public static StateDto from(State state) {
        return new StateDto(
            state.getId(),
            state.getName(),
            CountrySummaryDto.from(state.getCountry()),
            state.getCreatedDate()
        );
    }
}
