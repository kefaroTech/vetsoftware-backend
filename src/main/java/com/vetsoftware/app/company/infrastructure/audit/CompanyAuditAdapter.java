package com.vetsoftware.app.company.infrastructure.audit;

import com.vetsoftware.app.company.application.port.out.CompanyAuditPort;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import org.springframework.stereotype.Component;

/** Ver {@code SubscriptionBillingAuditAdapter}: misma funcion, otro modulo. */
@Component
public class CompanyAuditAdapter implements CompanyAuditPort {

    private final AuditLogger auditLogger;

    public CompanyAuditAdapter(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    public void companyReactivated(Long companyId, String companyName, String companyIdentifier) {
        auditLogger.companyReactivated(companyId, companyName, companyIdentifier);
    }
}
