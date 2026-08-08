package com.vetsoftware.app.auth.infrastructure.audit;

import com.vetsoftware.app.auth.application.port.out.SecurityEventPort;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import org.springframework.stereotype.Component;

/**
 * Lleva los eventos de seguridad de la capa de aplicación a la cadena de
 * auditoría.
 */
@Component
public class AuditSecurityEventPort implements SecurityEventPort {

    private final AuditLogger auditLogger;

    public AuditSecurityEventPort(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    public void refreshTokenReuseDetected(Long subjectId, String subjectType,
            long secondsSinceRevocation) {
        auditLogger.refreshTokenReuseDetected(subjectId, subjectType, secondsSinceRevocation);
    }
}
