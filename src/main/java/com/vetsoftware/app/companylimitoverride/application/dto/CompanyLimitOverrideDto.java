package com.vetsoftware.app.companylimitoverride.application.dto;

import com.vetsoftware.app.companylimitoverride.domain.CompanyLimitOverride;
import com.vetsoftware.app.companylimitoverride.domain.OverrideReasonCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** La excepción negociada tal como sale de la feature, con su firma dentro. */
public record CompanyLimitOverrideDto(Long id, Long companyId, Long limitDimensionId,
        int limitQuantity, LocalDate validFrom, LocalDate validTo, OverrideReasonCode reasonCode,
        String reason, Long grantedBySystemUserId, Long revokedBySystemUserId,
        LocalDateTime revokedAt, OverrideReasonCode revokedReasonCode, String revokedReason,
        boolean alive) {

    public static CompanyLimitOverrideDto from(CompanyLimitOverride override) {
        return new CompanyLimitOverrideDto(override.getId(), override.getCompanyId(),
                override.getLimitDimensionId(), override.getLimitQuantity(),
                override.getValidFrom(), override.getValidTo(), override.getReasonCode(),
                override.getReason(), override.getGrantedBySystemUserId(),
                override.getRevokedBySystemUserId(), override.getRevokedAt(),
                override.getRevokedReasonCode(), override.getRevokedReason(), override.isAlive());
    }
}
