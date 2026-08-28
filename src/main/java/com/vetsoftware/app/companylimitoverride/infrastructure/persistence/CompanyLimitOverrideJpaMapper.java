package com.vetsoftware.app.companylimitoverride.infrastructure.persistence;

import com.vetsoftware.app.companylimitoverride.domain.CompanyLimitOverride;
import com.vetsoftware.app.companylimitoverride.domain.OverrideReasonCode;
import org.springframework.stereotype.Component;

/** El único sitio que conoce a la vez la excepción de dominio y su fila. */
@Component
public class CompanyLimitOverrideJpaMapper {

    public CompanyLimitOverrideJpaEntity toJpa(CompanyLimitOverride override) {
        CompanyLimitOverrideJpaEntity entity = new CompanyLimitOverrideJpaEntity();
        entity.setId(override.getId());
        entity.setCompanyId(override.getCompanyId());
        entity.setLimitDimensionId(override.getLimitDimensionId());
        entity.setLimitQuantity(override.getLimitQuantity());
        entity.setValidFrom(override.getValidFrom());
        entity.setValidTo(override.getValidTo());
        entity.setReasonCode(override.getReasonCode().name());
        entity.setReason(override.getReason());
        entity.setGrantedBySystemUserId(override.getGrantedBySystemUserId());
        entity.setRevokedBySystemUserId(override.getRevokedBySystemUserId());
        entity.setRevokedAt(override.getRevokedAt());
        entity.setRevokedReasonCode(override.getRevokedReasonCode() == null
                ? null
                : override.getRevokedReasonCode().name());
        entity.setRevokedReason(override.getRevokedReason());
        entity.setCreatedDate(override.getCreatedDate());
        entity.setEnabled(override.isEnabled());
        entity.setVersion(override.getVersion());
        return entity;
    }

    public CompanyLimitOverride toDomain(CompanyLimitOverrideJpaEntity entity) {
        return new CompanyLimitOverride(entity.getId(), entity.getCompanyId(),
                entity.getLimitDimensionId(), entity.getLimitQuantity(), entity.getValidFrom(),
                entity.getValidTo(), OverrideReasonCode.valueOf(entity.getReasonCode()),
                entity.getReason(), entity.getGrantedBySystemUserId(),
                entity.getRevokedBySystemUserId(), entity.getRevokedAt(),
                entity.getRevokedReasonCode() == null
                        ? null
                        : OverrideReasonCode.valueOf(entity.getRevokedReasonCode()),
                entity.getRevokedReason(), entity.getCreatedDate(), entity.isEnabled(),
                entity.getVersion());
    }
}
