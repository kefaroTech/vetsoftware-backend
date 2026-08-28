package com.vetsoftware.app.companyentitlementsnapshot.domain;

import java.time.LocalDateTime;

/**
 * No hay ninguna foto anterior a ese momento. Casi siempre significa que la
 * empresa es anterior a la bitácora: la evidencia de antes no se puede
 * reconstruir, y por eso la tabla iba antes que los límites.
 */
public class CompanyEntitlementSnapshotNotFoundException extends RuntimeException {

    public CompanyEntitlementSnapshotNotFoundException(Long companyId, LocalDateTime at) {
        super("Company " + companyId + " has no entitlement snapshot at or before " + at);
    }
}
