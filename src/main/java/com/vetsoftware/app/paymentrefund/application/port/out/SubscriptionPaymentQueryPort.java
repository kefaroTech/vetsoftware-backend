package com.vetsoftware.app.paymentrefund.application.port.out;

import com.vetsoftware.app.paymentrefund.domain.SubscriptionPaymentRef;
import java.util.Optional;

/**
 * La FK compuesta {@code payment_refunds (company_id, payment_id)} contra
 * {@code subscription_payments}, que es de otra feature.
 *
 * <p>
 * Es un {@code QueryPort} y no un {@code ValidationPort} porque esta feature
 * <strong>si necesita un dato</strong> del pago: su importe, que es el techo de
 * lo que se puede devolver.
 *
 * <p>
 * Las dos operaciones van <strong>acotadas por empresa</strong>
 * ({@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}, BE-COV). La variante
 * ancha no se declara: con ella, una devolucion de la clinica A podria colgar
 * del pago de la clinica B -y la base la rechazaria por la FK compuesta, pero
 * el error saldria como un 500 de integridad en vez de como el «no existe» que
 * corresponde-.
 */
public interface SubscriptionPaymentQueryPort {

    Optional<SubscriptionPaymentRef> findByIdAndCompanyId(Long paymentId, Long companyId);

    /**
     * Toma un bloqueo pesimista sobre la fila del pago dentro de la transaccion en
     * curso, acotado por empresa.
     *
     * <p>
     * Es lo que serializa el <em>read-then-write</em> del tope: se toma antes de
     * sumar lo ya devuelto, de modo que dos devoluciones parciales concurrentes no
     * puedan leer la misma suma y pasar las dos. Sin el, el tope se cumple en las
     * pruebas de un solo hilo y se incumple en produccion sin dar un solo error.
     *
     * <p>
     * Sin resultado a proposito: el objetivo es el candado, no la fila. Si el pago
     * no es de esta empresa no se bloquea nada y la resolucion acotada posterior es
     * la que reporta el error.
     */
    void lockByIdAndCompanyId(Long paymentId, Long companyId);
}
