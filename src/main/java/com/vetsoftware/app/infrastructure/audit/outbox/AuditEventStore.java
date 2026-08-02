package com.vetsoftware.app.infrastructure.audit.outbox;

import java.util.Map;

/** Destino durable de los eventos de auditoría. */
public interface AuditEventStore {

  void append(String eventType, String outcome, Map<String, Object> attributes);
}
