package com.vetsoftware.app.vatfilingperiod.application.dto;

import com.vetsoftware.app.vatfilingperiod.domain.VatFilingFrequency;
import com.vetsoftware.app.vatfilingperiod.domain.VatFilingPeriod;
import java.time.LocalDateTime;

public record VatFilingPeriodDto(Long id, int fiscalYear, VatFilingFrequency frequency,
        String legalReference, LocalDateTime createdDate, boolean enabled) {

    public static VatFilingPeriodDto from(VatFilingPeriod period) {
        return new VatFilingPeriodDto(period.getId(), period.getFiscalYear(), period.getFrequency(),
                period.getLegalReference(), period.getCreatedDate(), period.isEnabled());
    }
}
