package com.vetsoftware.app.infrastructure.audit.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserta el evento en la misma transacción de negocio cuando existe; para eventos del borde abre
 * una transacción corta independiente del request.
 */
@Component
@ConditionalOnProperty(
        prefix = "vetsoftware.audit.outbox",
        name = "enabled",
        havingValue = "true")
final class JdbcAuditEventStore implements AuditEventStore {

    private static final Set<String> CONTEXT_KEYS = Set.of(
            "traceId", "spanId",
            MdcKeys.ACTOR_TYPE, MdcKeys.ACTOR_EMPLOYEE_ID, MdcKeys.ACTOR_COMPANY_ID,
            MdcKeys.ACTOR_SYSTEM_USER_ID, MdcKeys.CLIENT_IP, MdcKeys.USER_AGENT,
            MdcKeys.HTTP_METHOD, MdcKeys.HTTP_PATH);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcAuditEventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void append(String eventType, String outcome, Map<String, Object> attributes) {
        Instant now = Instant.now();
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("eventId", eventId);
        payload.put("event", eventType);
        payload.put("occurredAt", now.toString());
        payload.put("outcome", outcome);

        Map<String, String> context = MDC.getCopyOfContextMap();
        if (context != null) {
            context.forEach((key, value) -> {
                if (CONTEXT_KEYS.contains(key) && value != null && !value.isBlank()) {
                    payload.put(key, value);
                }
            });
        }
        payload.putAll(attributes);

        jdbcTemplate.update("""
                INSERT INTO audit_event_outbox
                    (event_id, event_type, payload, status, attempts, next_attempt_at, created_at)
                VALUES (?, ?, CAST(? AS JSON), 'PENDING', 0, ?, ?)
                """,
                eventId, eventType, serialize(payload), Timestamp.from(now), Timestamp.from(now));
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el evento de auditoría", exception);
        }
    }
}
