package com.vetsoftware.app.publicholiday.infrastructure.web.response;

import com.vetsoftware.app.publicholiday.application.dto.PublicHolidayDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PublicHolidayResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07-13", description = "Fecha observada: la que decide si el dia es habil") LocalDate holidayDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(example = "2026-07-09", description = "Efemeride antes del traslado de la Ley 51 de 1983") LocalDate nominalDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean moved,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String legalReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {

    public static PublicHolidayResponse from(PublicHolidayDto dto) {
        return new PublicHolidayResponse(dto.id(), dto.holidayDate(), dto.name(), dto.nominalDate(),
                dto.moved(), dto.legalReference(), dto.createdDate(), dto.enabled());
    }
}
