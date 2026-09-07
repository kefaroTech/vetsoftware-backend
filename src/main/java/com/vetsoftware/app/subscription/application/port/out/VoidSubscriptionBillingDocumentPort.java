package com.vetsoftware.app.subscription.application.port.out;

/**
 * Anula un documento de cobro abierto de un contrato que se sustituye.
 */
public interface VoidSubscriptionBillingDocumentPort {

    void voidDocument(Long companyId, Long documentId);
}
