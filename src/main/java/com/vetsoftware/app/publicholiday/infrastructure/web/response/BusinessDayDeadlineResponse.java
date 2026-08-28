package com.vetsoftware.app.publicholiday.infrastructure.web.response;

import com.vetsoftware.app.publicholiday.application.dto.BusinessDayDeadlineDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * El vencimiento de un plazo en dias habiles, tal como sale por HTTP.
 *
 * <p>
 * {@code weekdayHolidaysSkipped} viaja al front a proposito: es lo que permite
 * explicarle al usuario por que su plazo de quince dias habiles vencio
 * veintitres dias despues, en vez de dejarle sospechar del calculo.
 */
public record BusinessDayDeadlineResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07-01") LocalDate startDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "15") int businessDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07-23") LocalDate dueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Festivos entre semana que el plazo se salto") int weekdayHolidaysSkipped) {

    public static BusinessDayDeadlineResponse from(BusinessDayDeadlineDto dto) {
        return new BusinessDayDeadlineResponse(dto.startDate(), dto.businessDays(), dto.dueDate(),
                dto.weekdayHolidaysSkipped());
    }
}
