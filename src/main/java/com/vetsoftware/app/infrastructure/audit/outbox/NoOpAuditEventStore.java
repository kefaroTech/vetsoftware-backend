package com.vetsoftware.app.infrastructure.audit.outbox;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** En perfiles sin outbox, el evento sigue visible por el logger AUDIT. */
@Component
@ConditionalOnProperty(
    prefix = "vetsoftware.audit.outbox",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
final class NoOpAuditEventStore implements AuditEventStore {

  @Override
  public void append(String eventType, String outcome, Map<String, Object> attributes) {
    // La persistencia durable se habilita explícitamente en prod.
  }
}
