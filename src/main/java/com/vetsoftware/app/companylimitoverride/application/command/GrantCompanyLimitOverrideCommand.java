package com.vetsoftware.app.companylimitoverride.application.command;

import com.vetsoftware.app.companylimitoverride.domain.OverrideReasonCode;
import java.time.LocalDate;

/**
 * Negociar una excepción de techo.
 *
 * <p>
 * <strong>El motivo y la firma son obligatorios y no tienen valor por
 * defecto.</strong> Quien concede el pacto tiene que declararse: sin eso,
 * dentro de seis meses el informe de excepciones es una lista de números sin
 * nadie detrás.
 */
public record GrantCompanyLimitOverrideCommand(Long companyId, Long limitDimensionId,
        int limitQuantity, LocalDate validFrom, OverrideReasonCode reasonCode, String reason,
        Long grantedBySystemUserId) {
}
