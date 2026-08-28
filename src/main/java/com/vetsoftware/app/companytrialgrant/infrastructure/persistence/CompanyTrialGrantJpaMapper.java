package com.vetsoftware.app.companytrialgrant.infrastructure.persistence;

import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrant;
import com.vetsoftware.app.companytrialgrant.domain.TrialOutcome;
import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import org.springframework.stereotype.Component;

/** El único sitio que conoce a la vez la concesión de dominio y su fila. */
@Component
public class CompanyTrialGrantJpaMapper {

    public CompanyTrialGrantJpaEntity toJpa(CompanyTrialGrant grant) {
        CompanyTrialGrantJpaEntity entity = new CompanyTrialGrantJpaEntity();
        entity.setId(grant.getId());
        entity.setCompanyId(grant.getCompanyId());
        entity.setCatalogItemId(grant.getCatalogItemId());
        entity.setTrialWindowId(grant.getTrialWindowId());
        entity.setTrialWindowEndDate(grant.getTrialWindowEndDate());
        entity.setGrantedOn(grant.getGrantedOn());
        entity.setDaysGranted(grant.getDaysGranted());
        entity.setTrialEndDate(grant.getTrialEndDate());
        entity.setPolicyTrialDays(grant.getPolicyTrialDays());
        entity.setPolicyTrialOutcome(grant.getPolicyTrialOutcome().name());
        entity.setSourceQuoteId(grant.getSourceQuoteId());
        entity.setGrantingAmendmentId(grant.getGrantingAmendmentId());
        entity.setConsumedAt(grant.getConsumedAt());
        entity.setOutcome(grant.getOutcome() == null ? null : grant.getOutcome().name());
        entity.setCreatedDate(grant.getCreatedDate());
        entity.setVersion(grant.getVersion());
        return entity;
    }

    public CompanyTrialGrant toDomain(CompanyTrialGrantJpaEntity entity) {
        return new CompanyTrialGrant(entity.getId(), entity.getCompanyId(),
                entity.getCatalogItemId(), entity.getTrialWindowId(),
                entity.getTrialWindowEndDate(), entity.getGrantedOn(), entity.getDaysGranted(),
                entity.getTrialEndDate(), entity.getPolicyTrialDays(),
                TrialPolicyOutcome.valueOf(entity.getPolicyTrialOutcome()),
                entity.getSourceQuoteId(), entity.getGrantingAmendmentId(), entity.getConsumedAt(),
                entity.getOutcome() == null ? null : TrialOutcome.valueOf(entity.getOutcome()),
                entity.getCreatedDate(), entity.getVersion());
    }
}
