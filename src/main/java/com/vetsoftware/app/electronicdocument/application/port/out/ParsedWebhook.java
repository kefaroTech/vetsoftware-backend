package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.WebhookOutcome;

/**
 * Contenido de un webhook del proveedor ya normalizado: a qué documento aplica y qué sellos trae.
 */
public record ParsedWebhook(
    WebhookOutcome outcome,
    String providerDocumentKey,
    String prefix,
    Long consecutive,
    String cufe,
    String cude,
    String uuid,
    String xmlSigned,
    String qrData,
    String qrUrl,
    String pdfRepresentation,
    String rejectionReason) {}
