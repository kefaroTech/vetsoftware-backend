package com.vetsoftware.app.limitdimension.application.dto;

import com.vetsoftware.app.limitdimension.domain.LimitDimension;
import com.vetsoftware.app.limitdimension.domain.MeasureKind;
import com.vetsoftware.app.limitdimension.domain.SubModuleRef;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Lo que sale de la feature. */
public record LimitDimensionDto(Long id, String code, String name, MeasureKind measureKind,
        SubModuleRef subModule, Integer releaseDelayDays, LocalDate availableFrom,
        LocalDateTime createdDate) {

    public static LimitDimensionDto from(LimitDimension dimension) {
        return new LimitDimensionDto(dimension.getId(), dimension.getCode(), dimension.getName(),
                dimension.getMeasureKind(), dimension.getSubModule(),
                dimension.getReleaseDelayDays(), dimension.getAvailableFrom(),
                dimension.getCreatedDate());
    }
}
