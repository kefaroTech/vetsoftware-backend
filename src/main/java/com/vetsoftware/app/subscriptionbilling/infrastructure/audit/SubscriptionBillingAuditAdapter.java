package com.vetsoftware.app.subscriptionbilling.infrastructure.audit;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Ver {@code SubscriptionAuditAdapter}: misma función, otro módulo. */
@Component
public class SubscriptionBillingAuditAdapter implements SubscriptionBillingAuditPort {

    private final AuditLogger auditLogger;

    public SubscriptionBillingAuditAdapter(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    public void chargeAccrued(Long chargeId, Long subscriptionId, ChargeType chargeType,
            BigDecimal amount, Long amendmentId) {
        auditLogger.subscriptionChargeAccrued(chargeId, subscriptionId, name(chargeType), amount,
                amendmentId);
    }

    @Override
    public void chargeVoided(Long chargeId, Long compensationChargeId, Long subscriptionId,
            BigDecimal amount) {
        auditLogger.subscriptionChargeVoided(chargeId, compensationChargeId, subscriptionId,
                amount);
    }

    @Override
    public void documentIssued(Long documentId, String documentNumber, Long subscriptionId,
            IssueStatus issueStatus, BigDecimal amount, Integer chargeCount) {
        auditLogger.subscriptionDocumentIssued(documentId, documentNumber, subscriptionId,
                name(issueStatus), amount, chargeCount);
    }

    @Override
    public void documentVoided(Long documentId, String documentNumber, Long subscriptionId,
            String reason) {
        auditLogger.subscriptionDocumentVoided(documentId, documentNumber, subscriptionId, reason);
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
