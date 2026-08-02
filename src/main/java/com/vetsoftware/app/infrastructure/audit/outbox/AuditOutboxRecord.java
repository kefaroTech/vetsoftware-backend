package com.vetsoftware.app.infrastructure.audit.outbox;

/**
 * Fila reclamada para publicar. Los cuatro últimos campos son la prueba de integridad que viaja con
 * el evento hasta el archivo inmutable.
 */
record AuditOutboxRecord(
    long id,
    String eventId,
    String payload,
    int attempts,
    long chainSequence,
    String previousHash,
    String payloadHash,
    String chainHash) {}
