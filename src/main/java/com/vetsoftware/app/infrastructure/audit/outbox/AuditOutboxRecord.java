package com.vetsoftware.app.infrastructure.audit.outbox;

record AuditOutboxRecord(long id, String eventId, String payload, int attempts) {}
