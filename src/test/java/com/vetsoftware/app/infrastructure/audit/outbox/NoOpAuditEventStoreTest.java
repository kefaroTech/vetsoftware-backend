package com.vetsoftware.app.infrastructure.audit.outbox;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * En perfiles sin outbox habilitado el evento sigue visible por el logger AUDIT
 * (ver {@link com.vetsoftware.app.infrastructure.audit.AuditLogger}); este
 * adaptador es deliberadamente un no-op — la única invariante que le
 * corresponde es no lanzar, sea cual sea el contenido.
 */
@DisplayName("NoOpAuditEventStore")
class NoOpAuditEventStoreTest {

    private final NoOpAuditEventStore store = new NoOpAuditEventStore();

    @Test
    @DisplayName("append no lanza y no persiste nada, con atributos presentes")
    void append_no_lanza_con_atributos_presentes() {
        assertThatCode(
                () -> store.append("login_success", "SUCCESS", Map.of("actor.type", "EMPLOYEE")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("append no lanza con un mapa de atributos vacío")
    void append_no_lanza_con_atributos_vacios() {
        assertThatCode(() -> store.append("rate_limited", "DENIED", Map.of()))
                .doesNotThrowAnyException();
    }
}
