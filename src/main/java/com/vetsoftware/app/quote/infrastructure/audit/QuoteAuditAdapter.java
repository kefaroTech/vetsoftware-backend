package com.vetsoftware.app.quote.infrastructure.audit;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.quote.application.port.out.QuoteAuditPort;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class QuoteAuditAdapter implements QuoteAuditPort {

    private final AuditLogger auditLogger;

    public QuoteAuditAdapter(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    public void quoteAccepted(Long quoteId, String quoteNumber, Long companyId, Long subscriptionId,
            BigDecimal totalAmount, String currency, String acceptedByEmail, String acceptedIp) {
        auditLogger.quoteAccepted(quoteId, quoteNumber, companyId, subscriptionId, totalAmount,
                currency, acceptedByEmail, acceptedIp);
    }
}
