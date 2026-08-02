package com.vetsoftware.app.daycare.application.dto;

import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.daycare.domain.DayCareType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DayCareDto(Long id, LocalDate date, LocalDate startDate, LocalDate endDate,
        DayCareType type, String objects, String observations, AnimalSummaryDto animal,
        CompanySummaryDto company, LocalDateTime createdDate, boolean enabled) {
    public static DayCareDto from(DayCare dayCare) {
        return new DayCareDto(dayCare.getId(), dayCare.getDate(), dayCare.getStartDate(),
                dayCare.getEndDate(), dayCare.getType(), dayCare.getObjects(),
                dayCare.getObservations(), AnimalSummaryDto.from(dayCare.getAnimal()),
                CompanySummaryDto.from(dayCare.getCompany()), dayCare.getCreatedDate(),
                dayCare.isEnabled());
    }
}
