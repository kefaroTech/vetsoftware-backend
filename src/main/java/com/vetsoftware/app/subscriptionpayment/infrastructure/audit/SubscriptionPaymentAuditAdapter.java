package com.vetsoftware.app.subscriptionpayment.infrastructure.audit;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentAuditPort;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Ver {@code SubscriptionAuditAdapter}: misma función, otro módulo. */
@Component
public class SubscriptionPaymentAuditAdapter implements SubscriptionPaymentAuditPort {

    private final AuditLogger auditLogger;

    public SubscriptionPaymentAuditAdapter(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    public void paymentRegistered(Long paymentId, PaymentMethod method, BigDecimal amount,
            String currency, String gateway, String gatewayReference,
            SubscriptionPaymentStatus status) {
        auditLogger.subscriptionPaymentRegistered(paymentId, name(method), amount, currency,
                gateway, gatewayReference, name(status));
    }

    @Override
    public void paymentStatusChanged(Long paymentId, SubscriptionPaymentStatus fromStatus,
            SubscriptionPaymentStatus toStatus, BigDecimal amount, String currency, String gateway,
            String gatewayReference) {
        auditLogger.subscriptionPaymentStatusChanged(paymentId, name(fromStatus), name(toStatus),
                amount, currency, gateway, gatewayReference);
    }

    @Override
    public void documentApplied(Long applicationId, Long documentId,
            ApplicationSourceKind sourceKind, BigDecimal amount) {
        auditLogger.subscriptionDocumentApplied(applicationId, documentId, name(sourceKind),
                amount);
    }

    @Override
    public void applicationReversed(Long applicationId, Long documentId, BigDecimal amount,
            String reason) {
        auditLogger.subscriptionApplicationReversed(applicationId, documentId, amount, reason);
    }

    @Override
    public void overpaymentCredited(Long paymentId, Long documentId, BigDecimal amount) {
        auditLogger.subscriptionPaymentOverpaymentCredited(paymentId, documentId, amount);
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
