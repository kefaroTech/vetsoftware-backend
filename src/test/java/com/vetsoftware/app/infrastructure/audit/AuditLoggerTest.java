package com.vetsoftware.app.infrastructure.audit;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.infrastructure.audit.outbox.AuditEventStore;
import org.junit.jupiter.api.Test;

class AuditLoggerTest {

    private final AuditEventStore eventStore = mock(AuditEventStore.class);
    private final AuditLogger auditLogger = new AuditLogger(eventStore);

    @Test
    void mutationPersistsACompleteDurableEvent() {
        auditLogger.mutation("PATCH", "/api/v1/animals/9", 200, "SUCCESS", 37);

        verify(eventStore).append(eq("http_mutation"), eq("SUCCESS"),
                argThat(attributes -> attributes.get("http.method").equals("PATCH")
                        && attributes.get("http.path").equals("/api/v1/animals/9")
                        && attributes.get("http.status").equals(200)
                        && attributes.get("http.durationMs").equals(37L)));
    }

    @Test
    void securityEventIsAlsoPersisted() {
        auditLogger.unauthenticated("GET", "/api/v1/owners", "invalid_token");

        verify(eventStore).append(eq("unauthenticated"), eq("DENIED"),
                argThat(attributes -> attributes.get("http.method").equals("GET")
                        && attributes.get("http.path").equals("/api/v1/owners")
                        && attributes.get("reason").equals("invalid_token")));
    }
}
