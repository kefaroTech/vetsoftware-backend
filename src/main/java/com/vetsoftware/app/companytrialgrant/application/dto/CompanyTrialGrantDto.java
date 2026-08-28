package com.vetsoftware.app.companytrialgrant.application.dto;

import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrant;
import com.vetsoftware.app.companytrialgrant.domain.TrialOutcome;
import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La concesión tal como sale de la feature.
 *
 * <p>
 * Lleva {@code effectiveDays} y no solo {@code daysGranted}: son números
 * distintos en cuanto la ventana recorta, y enseñar el segundo cuando el real
 * es el primero es prometerle al cliente días que no va a tener.
 */
public record CompanyTrialGrantDto(Long id, Long companyId, Long catalogItemId, Long trialWindowId,
        LocalDate grantedOn, int daysGranted, int effectiveDays, LocalDate trialEndDate,
        int policyTrialDays, TrialPolicyOutcome policyTrialOutcome, Long sourceQuoteId,
        Long grantingAmendmentId, LocalDateTime consumedAt, TrialOutcome outcome, boolean live) {

    public static CompanyTrialGrantDto from(CompanyTrialGrant grant) {
        return new CompanyTrialGrantDto(grant.getId(), grant.getCompanyId(),
                grant.getCatalogItemId(), grant.getTrialWindowId(), grant.getGrantedOn(),
                grant.getDaysGranted(), grant.effectiveDays(), grant.getTrialEndDate(),
                grant.getPolicyTrialDays(), grant.getPolicyTrialOutcome(), grant.getSourceQuoteId(),
                grant.getGrantingAmendmentId(), grant.getConsumedAt(), grant.getOutcome(),
                grant.isLive());
    }
}
