package com.vetsoftware.app.customercredit.infrastructure.audit;

import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditAuditPort;
import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CustomerCreditAuditAdapter implements CustomerCreditAuditPort {

    private final AuditLogger auditLogger;

    public CustomerCreditAuditAdapter(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    public void granted(Long entryId, Long companyId, BigDecimal amount,
            CreditOriginKind originKind, Long originPaymentId, Long originDocumentId,
            Long originSubscriptionId) {
        auditLogger.customerCreditGranted(entryId, companyId, amount,
                originKind == null ? null : originKind.name(), originPaymentId, originDocumentId,
                originSubscriptionId);
    }
}
