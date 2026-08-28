package com.vetsoftware.app.subscriptionpayment.application.port.out;

import com.vetsoftware.app.subscriptionpayment.domain.CustomerCreditLotRef;
import java.util.Optional;

/**
 * Resuelve —y deja bloqueado— el lote de saldo a favor del que sale lo que se
 * aplica.
 *
 * <p>
 * <b>Resuelve un lote, no un saldo.</b> Aplicar contra una factura consume de
 * un {@code GRANT} concreto, porque de una suma no se puede decir cual caduca
 * antes — y sin eso la caducidad del saldo a favor no es calculable.
 *
 * <p>
 * <b>No hay variante que solo lea, y es a proposito.</b> El unico caso de uso
 * que llega hasta aqui es {@code ApplyBillingDocumentService}, y ahi la lectura
 * del lote es la mitad de un <em>read-then-write</em>: se lee cuanto se
 * concedio para decidir si cabe una aplicacion mas. Ofrecer un
 * {@code findLot...} sin candado seria dejar a mano la version insegura de la
 * unica operacion que existe — el mismo criterio con el que
 * {@code CompanyBillingProfileRepository} no declara la variante ancha de sus
 * consultas.
 *
 * <p>
 * Solo variante acotada por empresa
 * ({@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}).
 */
public interface CustomerCreditQueryPort {

    /**
     * El lote, si existe, es de esa empresa <b>y es un {@code GRANT}</b>, con su
     * fila tomada en <b>bloqueo pesimista</b> hasta el fin de la transaccion.
     *
     * <p>
     * <b>El candado es lo unico que serializa dos aplicaciones del mismo lote.</b>
     * A diferencia de una retencion —que pertenece a una sola factura y queda
     * serializada por el candado de esa factura—, un lote de saldo a favor se puede
     * aplicar a dos facturas distintas a la vez: sus dos transacciones bloquean
     * documentos <em>distintos</em> y no se estorban. Sin este candado las dos leen
     * la misma suma aplicada, las dos pasan el techo de R3 y el lote se gasta de
     * mas.
     *
     * <p>
     * Va acotado por empresa por lo mismo que
     * {@code SubscriptionPaymentJpaRepository.lockByIdAndCompanyId}: la variante
     * ancha concederia un bloqueo pesimista sobre la fila de otro tenant antes de
     * cualquier comprobacion. Lo soltaria el rollback, pero se habria concedido.
     */
    Optional<CustomerCreditLotRef> lockLotByIdAndCompanyId(Long creditEntryId, Long companyId);
}
