package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.application.dto.ContractPaymentOutcome;
import com.vetsoftware.app.subscription.domain.BillingCycle;

/**
 * <strong>&#9940; AQUI ENTRA LA PASARELA DE PAGO. Este puerto es el hueco, y
 * esta vacio a proposito.</strong>
 *
 * <p>
 * Cobrar el primer periodo de un contrato recien firmado. Hoy lo implementa
 * {@code SimulatedContractPaymentAdapter}, que aprueba siempre porque
 * <strong>todavia no hay pasarela contratada</strong>. Conectar el cobro real
 * es escribir un segundo adaptador de esta interfaz y retirar el simulado:
 * <strong>ni el caso de uso ni el contrato de la API cambian</strong>. Esa es
 * toda la razon de que esto sea un puerto y no una fila escrita a mano en algun
 * servicio.
 *
 * <p>
 * <strong>Lo que el adaptador real tendra que respetar, y no es
 * evidente:</strong>
 *
 * <ul>
 * <li><b>No se puede llamar dentro de una transaccion.</b> Una llamada HTTP
 * retiene la conexion del pool y los locks mientras dura, y la regla dura
 * {@code SIN_IO_EXTERNO_EN_TRANSACCION} sigue la cadena de llamadas hasta aqui.
 * Por eso {@code SettleNewContractService} <strong>no</strong> lleva
 * {@code @Transactional} y por eso su llamador lo invoca en
 * {@code afterCommit}: el contrato ya esta firmado y confirmado cuando se
 * intenta el cobro.</li>
 * <li><b>Un rechazo no es una excepcion.</b> Se devuelve
 * {@link ContractPaymentOutcome#declined(String)} y el contrato se queda donde
 * nacio. Lanzar dejaria el contrato firmado y al llamador convencido de que
 * algo se rompio, cuando lo que paso es que la tarjeta no paso.</li>
 * <li><b>Idempotencia por contrato.</b> El id del contrato es la llave natural
 * del primer cobro; un reintento no puede cobrar dos veces.</li>
 * </ul>
 *
 * <p>
 * <strong>Lo que este puerto NO es.</strong> No registra el pago ni lo aplica a
 * ningun documento: eso es {@code subscriptionpayment}, que ya tiene su modelo
 * completo. Este puerto solo responde «¿puedo activar el contrato?».
 */
public interface ContractPaymentPort {

    /**
     * Intenta cobrar el primer periodo. Nunca lanza por un rechazo comercial: un
     * «no» es un {@link ContractPaymentOutcome} con {@code approved = false}.
     */
    ContractPaymentOutcome chargeFirstPeriod(Long companyId, Long subscriptionId,
            String subscriptionNumber, BillingCycle billingCycle);
}
