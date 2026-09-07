package com.vetsoftware.app.subscription.application.port.out;

import java.util.List;

/**
 * Los documentos de cobro abiertos con saldo de un contrato.
 */
public interface SubscriptionOpenDocumentsQueryPort {

    List<Long> findOpenDocumentIdsWithBalance(Long companyId, Long subscriptionId);
}
