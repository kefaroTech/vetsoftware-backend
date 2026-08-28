package com.vetsoftware.app.companylimitoverride.application.command;

import com.vetsoftware.app.companylimitoverride.domain.OverrideReasonCode;

/** Cerrar una excepción negociada, con quién la quita y por qué. */
public record RevokeCompanyLimitOverrideCommand(Long companyId, Long limitDimensionId,
        Long revokedBySystemUserId, OverrideReasonCode revokedReasonCode, String revokedReason) {
}
