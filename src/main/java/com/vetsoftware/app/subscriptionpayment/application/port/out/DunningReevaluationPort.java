package com.vetsoftware.app.subscriptionpayment.application.port.out;

/** Reevalúa la mora después de cualquier cambio del saldo de una factura. */
public interface DunningReevaluationPort {
    void reevaluate(Long billingDocumentId, Long companyId);
}
