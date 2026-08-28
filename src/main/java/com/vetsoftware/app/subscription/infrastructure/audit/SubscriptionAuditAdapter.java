package com.vetsoftware.app.subscription.infrastructure.audit;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAuditPort;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Traduce los hechos del contrato a eventos del canal {@code AUDIT}.
 *
 * <p>
 * Sin lógica y a propósito: el adaptador solo existe para que
 * {@code ..application..} no dependa de {@code ..infrastructure..}. Los enums
 * salen con {@code name()} —mayúsculas, la forma que ya usa el resto del canal—
 * y no en minúsculas como en las métricas: son dos audiencias distintas, el log
 * se lee y la etiqueta se agrupa.
 */
@Component
public class SubscriptionAuditAdapter implements SubscriptionAuditPort {

    private final AuditLogger auditLogger;

    public SubscriptionAuditAdapter(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    public void itemAdded(Long subscriptionId, Long itemId, Long catalogItemId, Integer quantity,
            BigDecimal monthlyDeltaAmount, Long amendmentId) {
        auditLogger.subscriptionItemAdded(subscriptionId, itemId, catalogItemId, quantity,
                monthlyDeltaAmount, amendmentId);
    }

    @Override
    public void itemRemoved(Long subscriptionId, Long itemId, BigDecimal monthlyDeltaAmount,
            Long amendmentId) {
        auditLogger.subscriptionItemRemoved(subscriptionId, itemId, monthlyDeltaAmount,
                amendmentId);
    }

    @Override
    public void itemQuantityChanged(Long subscriptionId, Long itemId, Integer previousQuantity,
            Integer newQuantity, BigDecimal monthlyDeltaAmount, Long amendmentId) {
        auditLogger.subscriptionItemQuantityChanged(subscriptionId, itemId, previousQuantity,
                newQuantity, monthlyDeltaAmount, amendmentId);
    }

    @Override
    public void statusChanged(Long subscriptionId, SubscriptionStatus fromStatus,
            SubscriptionStatus toStatus, SubscriptionStatusChangeReason reason) {
        // El motivo sale en minusculas y los estados en mayusculas a proposito: el
        // vocabulario del motivo estaba documentado asi desde el principio y es lo
        // que ya consultan las busquedas guardadas del canal.
        auditLogger.subscriptionStatusChanged(subscriptionId, name(fromStatus), name(toStatus),
                reason == null ? null : reason.code());
    }

    @Override
    public void cancellationRequested(Long subscriptionId, LocalDate effectiveOn) {
        auditLogger.subscriptionCancellationRequested(subscriptionId,
                Objects.toString(effectiveOn, null));
    }

    @Override
    public void entitlementsRecalculated(Long companyId, String triggerReason) {
        // permissionRows no viaja: contarlas exigiria una consulta extra por cada
        // cambio de contrato, y la pregunta que responde -«cuantos permisos quedaron»-
        // se contesta mejor leyendo la tabla que estimandola desde un log.
        auditLogger.companyEntitlementsRecalculated(companyId, triggerReason, null);
    }

    private static String name(SubscriptionStatus status) {
        return status == null ? null : status.name();
    }
}
