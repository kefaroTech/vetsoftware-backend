package com.vetsoftware.app.paymentrefund.domain;

import java.math.BigDecimal;

/**
 * Companion VO del pago que vive en {@code subscriptionpayment}.
 *
 * <p>
 * Es una <strong>copia</strong> de los tres datos que esta feature necesita, no
 * una referencia al agregado ajeno: el vertical slicing prohibe importar el
 * dominio de otra feature, y {@code SubscriptionPaymentQueryPort} es el unico
 * punto que conoce la otra rodaja.
 *
 * <p>
 * <strong>El {@code amount} es la razon de ser de este VO.</strong> Sin el, el
 * tope de la devolucion no se puede comprobar en el dominio y acabaria en el
 * service, que es donde {@code CLAUDE.md} dice que no van las invariantes.
 *
 * <p>
 * <strong>{@code companyId} no es decorativo</strong>: es la mitad de la clave
 * compuesta {@code (company_id, id)} con la que
 * {@code fk_payment_refunds_payment} impide que una devolucion de una clinica
 * cuelgue del pago de otra. El constructor lo exige para que un {@code Ref}
 * construido a mano no pueda saltarse la comprobacion.
 *
 * <p>
 * Este VO <strong>no se guarda</strong>: {@link PaymentRefund} conserva solo el
 * {@code paymentId}. Se pasa a la factoria, se usa para validar y se descarta —
 * asi el camino de lectura del mapper no necesita rehidratar el pago para
 * reconstruir una devolucion ya escrita.
 */
public record SubscriptionPaymentRef(Long id, Long companyId, BigDecimal amount) {

    public SubscriptionPaymentRef {
        if (id == null)
            throw new IllegalArgumentException("payment id is required");
        if (companyId == null)
            throw new IllegalArgumentException("payment company id is required");
        if (amount == null)
            throw new IllegalArgumentException("payment amount is required");
        if (amount.signum() <= 0)
            throw new IllegalArgumentException("payment amount must be greater than zero");
    }
}
