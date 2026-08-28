package com.vetsoftware.app.companyentitlementsnapshot.infrastructure.persistence;

import com.vetsoftware.app.companyentitlementsnapshot.domain.CompanyEntitlementSnapshot;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotActor;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotTriggerReason;
import org.springframework.stereotype.Component;

/** El único sitio que conoce a la vez la foto de dominio y su fila. */
@Component
public class CompanyEntitlementSnapshotJpaMapper {

    public CompanyEntitlementSnapshotJpaEntity toJpa(CompanyEntitlementSnapshot snapshot) {
        CompanyEntitlementSnapshotJpaEntity entity = new CompanyEntitlementSnapshotJpaEntity();
        entity.setId(snapshot.getId());
        entity.setCompanyId(snapshot.getCompanyId());
        entity.setRecalculatedAt(snapshot.getRecalculatedAt());
        entity.setActorEmployeeId(snapshot.getActor().employeeId());
        entity.setActorSystemUserId(snapshot.getActor().systemUserId());
        entity.setActorIsProcess(snapshot.getActor().process());
        entity.setTriggerReason(snapshot.getTriggerReason().name());
        entity.setAmendmentId(snapshot.getAmendmentId());
        entity.setPayload(snapshot.getPayload());
        entity.setPayloadFormatVersion(snapshot.getPayloadFormatVersion());
        entity.setCreatedDate(snapshot.getCreatedDate());
        return entity;
    }

    public CompanyEntitlementSnapshot toDomain(CompanyEntitlementSnapshotJpaEntity entity) {
        SnapshotActor actor = new SnapshotActor(entity.getActorEmployeeId(),
                entity.getActorSystemUserId(), entity.isActorIsProcess());
        return new CompanyEntitlementSnapshot(entity.getId(), entity.getCompanyId(),
                entity.getRecalculatedAt(), actor,
                SnapshotTriggerReason.valueOf(entity.getTriggerReason()), entity.getAmendmentId(),
                entity.getPayload(), entity.getPayloadFormatVersion(), entity.getCreatedDate());
    }
}
