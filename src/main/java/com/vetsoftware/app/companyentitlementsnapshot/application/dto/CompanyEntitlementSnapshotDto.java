package com.vetsoftware.app.companyentitlementsnapshot.application.dto;

import com.vetsoftware.app.companyentitlementsnapshot.domain.CompanyEntitlementSnapshot;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotTriggerReason;
import java.time.LocalDateTime;

/** La foto tal como sale de la feature. */
public record CompanyEntitlementSnapshotDto(Long id, Long companyId, LocalDateTime recalculatedAt,
        Long actorEmployeeId, Long actorSystemUserId, boolean actorIsProcess,
        SnapshotTriggerReason triggerReason, Long amendmentId, String payload,
        int payloadFormatVersion) {

    public static CompanyEntitlementSnapshotDto from(CompanyEntitlementSnapshot snapshot) {
        return new CompanyEntitlementSnapshotDto(snapshot.getId(), snapshot.getCompanyId(),
                snapshot.getRecalculatedAt(), snapshot.getActor().employeeId(),
                snapshot.getActor().systemUserId(), snapshot.getActor().process(),
                snapshot.getTriggerReason(), snapshot.getAmendmentId(), snapshot.getPayload(),
                snapshot.getPayloadFormatVersion());
    }
}
