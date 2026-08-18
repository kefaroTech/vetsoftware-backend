package com.vetsoftware.app.auth.infrastructure.audit;

import static org.mockito.Mockito.verify;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditSecurityEventPortTest {

    @Mock
    private AuditLogger auditLogger;
    @InjectMocks
    private AuditSecurityEventPort port;

    @Test
    @DisplayName("traslada el reuso de refresh token detectado a la cadena de auditoría")
    void traslada_el_reuso_detectado_a_la_auditoria() {
        port.refreshTokenReuseDetected(7L, "EMPLOYEE", 120L);

        verify(auditLogger).refreshTokenReuseDetected(7L, "EMPLOYEE", 120L);
    }
}
