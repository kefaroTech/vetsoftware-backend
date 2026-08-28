package com.vetsoftware.app.withholdingraterule.application.dto;

import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code version}</strong>: el numero de bloqueo optimista es una
 * barandilla del que escribe, no un dato de la tarifa. Publicarlo invitaria a
 * un cliente a enviarlo de vuelta y a construir un protocolo de concurrencia
 * que este catalogo —que solo escribe plataforma— no necesita.
 */
public record WithholdingRateRuleDto(Long id, WithholdingType withholdingType,
        ServiceNature serviceNature, String municipalityCode, BigDecimal ratePercent,
        BigDecimal minimumBaseAmount, BigDecimal minimumBaseUvt, String legalReference,
        LocalDate validFrom, LocalDate validTo, LocalDateTime createdDate, boolean enabled) {

    public static WithholdingRateRuleDto from(WithholdingRateRule rule) {
        return new WithholdingRateRuleDto(rule.getId(), rule.getWithholdingType(),
                rule.getServiceNature(), rule.getMunicipalityCode(), rule.getRatePercent(),
                rule.getMinimumBaseAmount(), rule.getMinimumBaseUvt(), rule.getLegalReference(),
                rule.getValidFrom(), rule.getValidTo(), rule.getCreatedDate(), rule.isEnabled());
    }
}
