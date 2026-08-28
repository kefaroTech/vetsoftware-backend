package com.vetsoftware.app.subscriptionbilling.application.dto;

/**
 * Resultado de una página del barrido de facturación recurrente.
 *
 * <p>
 * <b>Lleva cuatro contadores y no uno, porque «procesados» no distingue las
 * tres cosas que pueden pasarle a un contrato.</b> Un mes en el que todos los
 * contratos salen {@code skipped} —ya facturados por la vuelta anterior— es un
 * reinicio sano; un mes en el que salen {@code skipped} porque nadie devengó
 * nada es el cierre que se saltó a media plataforma. Con un solo número los dos
 * se leen igual.
 *
 * @param lastId
 *            cursor de la siguiente vuelta: el mayor id visto en esta
 */
public record SubscriptionBillingBatchResult(int processed, int documentsIssued, int chargesAccrued,
        int skipped, int failures, long lastId) {

    public SubscriptionBillingBatchResult {
        if (processed < 0 || documentsIssued < 0 || chargesAccrued < 0 || skipped < 0
                || failures < 0)
            throw new IllegalArgumentException("batch counters must not be negative");
        if (lastId < 0)
            throw new IllegalArgumentException("lastId must not be negative");
        if (documentsIssued + skipped + failures > processed)
            throw new IllegalArgumentException("a subscription cannot have more than one outcome:"
                    + " issued + skipped + failed must not exceed processed");
    }
}
