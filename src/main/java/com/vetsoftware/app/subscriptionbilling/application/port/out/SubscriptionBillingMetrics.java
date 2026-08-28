package com.vetsoftware.app.subscriptionbilling.application.port.out;

import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import java.math.BigDecimal;

/**
 * Telemetría de los hechos contables de la facturación de suscripciones: cargos
 * devengados y cuentas de cobro emitidas.
 *
 * <p>
 * <b>No es una fuente contable</b> y no sustituye a
 * {@code subscription_charges} ni a {@code subscription_billing_documents}.
 * Existe para responder, sin abrir la base de producción, las dos preguntas del
 * cierre de mes: <i>¿cuántos cargos se emitieron anoche y por cuánto?</i> y
 * <i>¿se emitieron dos veces?</i>. Con lo que había —solo latencia de las
 * lecturas— la única señal de que el barrido de facturación se reinició a mitad
 * del lote y volvió a crear los cargos que ya había creado era que el contador
 * de ejecuciones del job registraba dos incrementos en vez de uno, y la
 * anomalía se descubría cuando un cliente reclamaba la factura del mes
 * siguiente.
 *
 * <p>
 * <b>Sin empresa.</b> Ninguna firma admite un {@code companyId} y no debe
 * admitirlo: con 500 clínicas sería multiplicar cada serie por 500. La empresa
 * está en el MDC y en el span.
 */
public interface SubscriptionBillingMetrics {

    /**
     * Se devengó un cargo. {@code amount} es el subtotal con su signo: una
     * proración de reducción y una nota de crédito son negativas, y sumarlas en
     * valor absoluto convertiría un abono en una venta.
     */
    void chargeAccrued(ChargeType chargeType, BigDecimal amount);

    /**
     * Se anuló un cargo emitiendo su compensación. Se cuenta con
     * {@code result=cancelled} sobre el mismo contador que el alta, no en una
     * métrica aparte: la pregunta operativa es la proporción entre lo devengado y
     * lo anulado, y separar las series obliga a cruzarlas a mano en cada panel.
     */
    void chargeVoided(ChargeType chargeType);

    /** Se emitió una cuenta de cobro. */
    void documentIssued(IssueStatus issueStatus);

    /** Se anuló una cuenta de cobro antes de existir fuera. */
    void documentVoided(IssueStatus issueStatus);

    /**
     * La emisión no llegó a ocurrir. {@code Rejection} es un vocabulario cerrado y
     * corto a propósito: distingue el ciclo ya facturado —que es la barandilla
     * antiduplicados haciendo su trabajo, y por tanto una buena noticia— del
     * documento sin cargos, que significa que el devengo no corrió.
     */
    void documentRejected(Rejection rejection);

    /**
     * Los dos desenlaces no felices de la emisión, con el mismo vocabulario que ya
     * usa el resto del catálogo de métricas de negocio.
     */
    enum Rejection {
        /** Ya existía la factura de ciclo de ese periodo exacto. */
        DUPLICATE_CYCLE("duplicate_ignored"),
        /** No había ni un cargo pendiente que facturar en el periodo. */
        NO_CHARGES("rejected");

        private final String value;

        Rejection(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
