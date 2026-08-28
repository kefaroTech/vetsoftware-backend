package com.vetsoftware.app.uvtvalue.application.dto;

import com.vetsoftware.app.uvtvalue.domain.UvtValue;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UvtValueDto(Long id, int fiscalYear, BigDecimal valueAmount, String legalReference,
        LocalDateTime createdDate, boolean enabled) {

    public static UvtValueDto from(UvtValue value) {
        return new UvtValueDto(value.getId(), value.getFiscalYear(), value.getValueAmount(),
                value.getLegalReference(), value.getCreatedDate(), value.isEnabled());
    }
}
