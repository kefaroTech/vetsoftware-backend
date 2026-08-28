package com.vetsoftware.app.companyentitlementsnapshot.application.command;

import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotActor;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotTriggerReason;

/**
 * Guardar la foto de un recálculo.
 *
 * <p>
 * El {@code payload} llega ya serializado: esta rodaja no conoce la forma de
 * los permisos —por eso el documento es JSON y no columnas—, y así la bitácora
 * no se rompe cada vez que esa tabla evoluciona.
 */
public record RecordEntitlementSnapshotCommand(Long companyId, SnapshotActor actor,
        SnapshotTriggerReason triggerReason, Long amendmentId, String payload,
        int payloadFormatVersion) {
}
