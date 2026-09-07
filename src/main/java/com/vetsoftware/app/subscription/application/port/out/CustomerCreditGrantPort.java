package com.vetsoftware.app.subscription.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CustomerCreditGrantPort {

    /**
     * Concede saldo a favor por el tramo de {@code subscriptionId} que el cliente
     * pago y no va a consumir. {@code amount} viaja en positivo: es el origen quien
     * decide con que signo se prorratea, no este puerto.
     *
     * @return el id del lote concedido: lo necesita
     *         {@code ReplaceSubscriptionFromQuoteService} para aplicarlo despues al
     *         documento del primer periodo del contrato nuevo
     */
    Long grantForUnusedPeriod(Long companyId, Long subscriptionId, BigDecimal amount,
            LocalDate today, String clientRequestId);
}
