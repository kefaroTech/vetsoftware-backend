package com.vetsoftware.app.subscriptionbilling.application.port.out;

import java.util.List;

/**
 * Si un documento de cobro tiene aplicaciones de pago en vuelo, de otra feature
 * ({@code subscriptionpayment}), vistas desde aquí solo para decidir si se
 * puede anular.
 */
public interface PendingPaymentApplicationQueryPort {

    boolean existsPendingApplication(Long companyId, Long billingDocumentId);

    /**
     * Ids de las aplicaciones CONFIRMED sobre este documento, a revertir antes de
     * anular.
     */
    List<Long> findConfirmedApplicationIds(Long companyId, Long billingDocumentId);
}
