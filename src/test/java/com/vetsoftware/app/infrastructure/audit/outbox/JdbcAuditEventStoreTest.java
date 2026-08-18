package com.vetsoftware.app.infrastructure.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.audit.chain.AuditChainHash;
import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import java.sql.Timestamp;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcAuditEventStore")
class JdbcAuditEventStoreTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private JdbcAuditEventStore store;

    @BeforeEach
    void limpiaElMdc() {
        MDC.clear();
    }

    @AfterEach
    void limpiaElMdcAlTerminar() {
        MDC.clear();
    }

    @Nested
    @DisplayName("append")
    class Append {

        @Test
        @DisplayName("persiste el payload base con el hash del contenido serializado")
        void persiste_el_payload_base_con_el_hash_del_contenido() {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"event\":\"e\"}");

            store.append("http_mutation", "SUCCESS", Map.of("http.status", 201));

            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(objectMapper).writeValueAsString(payloadCaptor.capture());
            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload).containsEntry("schemaVersion", 1)
                    .containsEntry("event", "http_mutation").containsEntry("outcome", "SUCCESS")
                    .containsEntry("http.status", 201);
            assertThat(payload).containsKeys("eventId", "occurredAt");

            String expectedHash = AuditChainHash.payloadHash("{\"event\":\"e\"}");
            verify(jdbcTemplate).update(
                    org.mockito.ArgumentMatchers.contains("INSERT INTO audit_event_outbox"),
                    eq(payload.get("eventId")), eq("http_mutation"), eq("{\"event\":\"e\"}"),
                    eq(expectedHash), any(Timestamp.class), any(Timestamp.class));
        }

        @Test
        @DisplayName("no incluye contexto cuando el MDC esta vacio")
        void no_incluye_contexto_cuando_el_mdc_esta_vacio() {
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            store.append("evento", "SUCCESS", Map.of());

            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(objectMapper).writeValueAsString(payloadCaptor.capture());
            assertThat(payloadCaptor.getValue()).doesNotContainKeys("traceId", MdcKeys.ACTOR_TYPE,
                    MdcKeys.CLIENT_IP);
        }

        @Test
        @DisplayName("incluye del MDC solo las claves permitidas con valor no vacio")
        void incluye_del_mdc_solo_las_claves_permitidas_con_valor_no_vacio() {
            MDC.put("traceId", "trace-1");
            MDC.put(MdcKeys.ACTOR_TYPE, "EMPLOYEE");
            MDC.put(MdcKeys.CLIENT_IP, "   ");
            MDC.put("clave.no.permitida", "ignorame");
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            store.append("evento", "SUCCESS", Map.of());

            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(objectMapper).writeValueAsString(payloadCaptor.capture());
            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload).containsEntry("traceId", "trace-1")
                    .containsEntry(MdcKeys.ACTOR_TYPE, "EMPLOYEE");
            assertThat(payload).doesNotContainKeys(MdcKeys.CLIENT_IP, "clave.no.permitida");
        }

        @Test
        @DisplayName("lanza IllegalStateException si la serializacion falla, sin escribir en la tabla")
        void lanza_illegal_state_exception_si_la_serializacion_falla() {
            JacksonException failure = mock(JacksonException.class);
            when(objectMapper.writeValueAsString(any())).thenThrow(failure);

            assertThatThrownBy(() -> store.append("evento", "SUCCESS", Map.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No se pudo serializar el evento de auditoría");

            verifyNoInteractions(jdbcTemplate);
        }
    }
}
