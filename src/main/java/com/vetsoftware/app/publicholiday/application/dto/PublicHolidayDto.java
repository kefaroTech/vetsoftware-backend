package com.vetsoftware.app.publicholiday.application.dto;

import com.vetsoftware.app.publicholiday.domain.PublicHoliday;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PublicHolidayDto(Long id, LocalDate holidayDate, String name, LocalDate nominalDate,
        boolean moved, String legalReference, LocalDateTime createdDate, boolean enabled) {

    public static PublicHolidayDto from(PublicHoliday holiday) {
        return new PublicHolidayDto(holiday.getId(), holiday.getHolidayDate(), holiday.getName(),
                holiday.getNominalDate(), holiday.isMoved(), holiday.getLegalReference(),
                holiday.getCreatedDate(), holiday.isEnabled());
    }
}
