package com.vetsoftware.app.subscription.infrastructure.payment;

import com.vetsoftware.app.subscription.application.dto.ContractPaymentOutcome;
import com.vetsoftware.app.subscription.application.port.out.ContractPaymentPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <strong>&#9940; SIMULACION. Aprueba siempre y no mueve un solo peso.</strong>
 *
 * <p>
 * Existe porque el embudo comercial esta terminado y la pasarela de pago no
 * esta contratada. Sin esta clase el contrato nacido de una cotizacion aceptada
 * se quedaria para siempre en el estado en que nace y nadie podria probar el
 * camino completo; con ella, el flujo se recorre entero y el unico eslabon
 * postizo esta <strong>aislado en un fichero, detras de una interfaz</strong>.
 *
 * <p>
 * <strong>Como se retira.</strong> Se escribe el adaptador real de
 * {@link ContractPaymentPort} y se borra este fichero. No hay nada mas que
 * tocar: ni el caso de uso, ni el controller, ni {@code api/openapi.json}, ni
 * los dos fronts. Si alguien se ve modificando {@code SettleNewContractService}
 * para conectar la pasarela, se ha equivocado de sitio.
 *
 * <p>
 * <strong>La referencia lleva el prefijo {@code SIMULATED-} a
 * proposito.</strong> Es lo que hace que una fila de produccion que venga de
 * aqui se pueda encontrar con un {@code LIKE} el dia que se conecte el cobro
 * real, en vez de quedar mezclada con las de verdad. Un dato de mentira que no
 * se distingue del de verdad es peor que no tenerlo.
 *
 * <p>
 * <strong>Y deja rastro en {@code WARN}, no en {@code DEBUG}.</strong> Que un
 * contrato se active sin cobrar es exactamente la clase de hecho que nadie
 * quiere descubrir seis meses despues leyendo el codigo: tiene que verse en
 * Grafana desde el primer dia.
 */
@Component
public class SimulatedContractPaymentAdapter implements ContractPaymentPort {

    private static final Logger log = LoggerFactory
            .getLogger(SimulatedContractPaymentAdapter.class);

    /** Ver el javadoc de la clase: es lo que hace rastreable lo simulado. */
    private static final String REFERENCE_PREFIX = "SIMULATED-";

    @Override
    public ContractPaymentOutcome chargeFirstPeriod(Long companyId, Long subscriptionId,
            String subscriptionNumber, BillingCycle billingCycle) {
        log.warn(
                "Cobro SIMULADO del primer periodo: no se ha movido dinero."
                        + " contrato={} empresa={} ciclo={}",
                subscriptionNumber, companyId, billingCycle);
        return ContractPaymentOutcome.approved(REFERENCE_PREFIX + subscriptionId);
    }
}
