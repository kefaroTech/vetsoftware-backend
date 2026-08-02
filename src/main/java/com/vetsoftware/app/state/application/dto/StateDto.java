package com.vetsoftware.app.state.application.dto;

import com.vetsoftware.app.state.domain.State;
import java.time.LocalDateTime;

public record StateDto(Long id, String name, CountrySummaryDto country, String daneCode,
        LocalDateTime createdDate, boolean enabled) {
    public static StateDto from(State state) {
        return new StateDto(state.getId(), state.getName(),
                CountrySummaryDto.from(state.getCountry()), state.getDaneCode(),
                state.getCreatedDate(), state.isEnabled());
    }
}
